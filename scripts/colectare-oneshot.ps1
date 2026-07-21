<#
.SYNOPSIS
    Un ciclu complet de colectare golstat: infra -> API -> colector -> drenare Kafka -> oprire.

.DESCRIPTION
    Gandit pentru Windows Task Scheduler cu wake timers: PC-ul se trezeste din sleep, ruleaza acest
    script si (cu -Adoarme) revine in sleep. API-ul trebuie sa fie pornit in acelasi interval pentru
    ca el e cel care consuma din Kafka si scrie in Postgres — colectorul doar publica.

.PARAMETER Adoarme
    Readoarme laptopul la final, DAR numai daca noi l-am trezit (a iesit din sleep in ultimele
    -MinuteDeLaTrezire minute) si numai daca nu esti la tastatura (idle peste -PragIdleMinute).

    De ce ambele conditii: pe priza, planul de alimentare poate fi setat "nu adormi niciodata".
    Daca am adormi neconditionat, am schimba tacut o setare aleasa de utilizator — laptopul ar
    incepe sa se stinga la fiecare 3 ore. Asa, respectam regula: daca era deja pornit, ramane pornit.

.PARAMETER PragIdleMinute
    Cate minute de inactivitate sunt necesare ca -Adoarme sa aiba efect. Implicit 10.

.EXAMPLE
    .\colectare-oneshot.ps1
    .\colectare-oneshot.ps1 -Adoarme
#>
[CmdletBinding()]
param(
    [switch] $Adoarme,
    [int]    $PragIdleMinute = 10,
    [int]    $MinuteDeLaTrezire = 20,
    [int]    $MinInterMinute = 30
)

# Intentionat 'Continue', nu 'Stop': scriptul ruleaza multe executabile native (docker, gradlew) care
# scriu pe stderr in mod normal. In PowerShell 5.1, stderr de la un .exe devine ErrorRecord si cu
# 'Stop' ar arunca desi comanda a reusit. Verificam explicit $LASTEXITCODE si aruncam noi cu `throw`.
$ErrorActionPreference = 'Continue'

$Radacina  = Split-Path -Parent $PSScriptRoot
$Psql      = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
$DirLoguri = Join-Path $PSScriptRoot 'logs'
$Log       = Join-Path $DirLoguri ("colectare-{0}.log" -f (Get-Date -Format 'yyyy-MM-dd'))
$FisierPid = Join-Path $DirLoguri '.colectare.pid'
$FisierUlt = Join-Path $DirLoguri '.colectare-ultima.txt'

if (-not (Test-Path $DirLoguri)) { New-Item -ItemType Directory -Path $DirLoguri | Out-Null }

function Scrie($mesaj, $nivel = 'INFO') {
    $linie = "{0} [{1}] {2}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $nivel, $mesaj
    Write-Host $linie
    # Reincercam: daca altcineva tine fisierul deschis (un `tail -f`, alt editor), Add-Content arunca
    # si — cu output-ul redirectat de Task Scheduler — s-ar pierde TOT jurnalul rularii de noapte,
    # exact cand ai nevoie de el. Masurat pe 2026-07-20: s-a pierdut un ciclu intreg asa.
    for ($i = 0; $i -lt 5; $i++) {
        try {
            [System.IO.File]::AppendAllText($Log, $linie + [Environment]::NewLine, [System.Text.Encoding]::UTF8)
            return
        } catch {
            Start-Sleep -Milliseconds 150
        }
    }
    # Dupa 5 incercari renuntam in tacere: jurnalul nu are voie sa opreasca colectarea.
}

# Ruleaza o comanda native cu stderr inghitit de cmd (nu de PowerShell) si intoarce exit code-ul.
function Tacut([string] $comanda) {
    cmd /c "$comanda >nul 2>&1"
    return $LASTEXITCODE
}

# Asteapta pana cand $verifica intoarce $true. $true la reusita, $false la timeout.
function Asteapta($eticheta, [scriptblock] $verifica, [int] $secunde = 300) {
    $limita = (Get-Date).AddSeconds($secunde)
    while ((Get-Date) -lt $limita) {
        try { if (& $verifica) { Scrie "$eticheta - gata"; return $true } } catch { }
        Start-Sleep -Seconds 5
    }
    Scrie "$eticheta - TIMEOUT dupa ${secunde}s" 'EROARE'
    return $false
}

# Milisecunde de la ultima activitate de tastatura/mouse. Fara asta, -Adoarme ar putea stinge PC-ul
# exact cand lucrezi la el (task-ul poate porni si cand esti logat, nu doar dupa trezire).
if (-not ('GolstatIdle' -as [type])) {
    Add-Type @'
using System;
using System.Runtime.InteropServices;
public class GolstatIdle {
    [StructLayout(LayoutKind.Sequential)]
    private struct LASTINPUTINFO { public uint cbSize; public uint dwTime; }
    [DllImport("user32.dll")]
    private static extern bool GetLastInputInfo(ref LASTINPUTINFO plii);
    public static double MinuteIdle() {
        LASTINPUTINFO info = new LASTINPUTINFO();
        info.cbSize = (uint)Marshal.SizeOf(info);
        if (!GetLastInputInfo(ref info)) { return 0; }
        return ((uint)Environment.TickCount - info.dwTime) / 60000.0;
    }
}
'@
}

# A iesit laptopul din sleep in ultimele $minute? Adica: noi l-am trezit, sau era deja pornit?
# Sursa: Power-Troubleshooter scrie eveniment 1 la fiecare rezumare din sleep, cu "Wake Source".
# La eroare intoarcem $false — mai bine nu adormim decat sa adormim gresit.
function TrezitRecent([int] $minute) {
    try {
        $ev = Get-WinEvent -FilterHashtable @{
            LogName      = 'System'
            ProviderName = 'Microsoft-Windows-Power-Troubleshooter'
            Id           = 1
            StartTime    = (Get-Date).AddMinutes(-$minute)
        } -MaxEvents 1 -ErrorAction Stop
        return $null -ne $ev
    } catch {
        # niciun eveniment in interval => exceptie "No events were found", nu o eroare reala
        return $false
    }
}

$procesApi   = $null
$cod         = 0
$codColector = 0
$altCiclu    = $false

try {
    Scrie "=== Pornire ciclu (radacina: $Radacina) ==="

    # Un ciclu pornit deja? Doua colectoare simultan se bat pe portul 8082: al doilea moare la bind,
    # iese cu cod 1 si arata exact ca o colectare esuata, desi primul lucreaza normal. Task-ul are
    # doua triggere (orar + la deblocarea sesiunii), deci suprapunerea chiar se intampla — masurat
    # 2026-07-21, doua cicluri la 43 de secunde distanta. Anti-rularile-dese de mai jos NU acopera
    # cazul: marcajul lui se scrie abia la sfarsitul unui ciclu reusit.
    if (Test-Path $FisierPid) {
        $pidAlt = 0
        try { $pidAlt = [int](Get-Content $FisierPid -Raw).Trim() } catch { }
        if ($pidAlt -gt 0 -and $pidAlt -ne $PID -and
            (Get-Process -Id $pidAlt -ErrorAction SilentlyContinue)) {
            Scrie "Sarit: ciclul din PID $pidAlt inca ruleaza"
            $altCiclu = $true   # PID-ul e al LUI: nu-l stergem si nu adormim peste el
            return
        }
    }

    # Semnalizam ca rulam, ca adoarme-dupa-colectare.ps1 sa stie exact cand am terminat.
    # (Cautarea dupa linia de comanda dadea fals pozitiv: orice comanda care CONTINE numele
    # scriptului — inclusiv un diagnostic tastat in consola — parea o colectare in curs.)
    [System.IO.File]::WriteAllText($FisierPid, $PID)

    if (-not $env:API_FOOTBALL_KEY) {
        throw "API_FOOTBALL_KEY nu e setat. Seteaza-l ca variabila de mediu de UTILIZATOR (nu in script)."
    }
    # doar pentru verificarea de sanatate a bazei locale de dezvoltare
    if (-not $env:PGPASSWORD) { $env:PGPASSWORD = 'root' }

    # Anti-rulari-dese: trigger-ul pe deblocare poate porni de multe ori pe zi (fiecare unlock).
    # Daca un ciclu s-a terminat recent, nu mai are ce colecta (fixtures cache 1h) — sarim, dar tot
    # trecem prin `finally`, deci daca task-ul ne-a trezit, laptopul revine in sleep.
    if (Test-Path $FisierUlt) {
        $minDeLaUltima = ((Get-Date) - (Get-Item $FisierUlt).LastWriteTime).TotalMinutes
        if ($minDeLaUltima -lt $MinInterMinute) {
            Scrie ("Sarit: ultimul ciclu a fost acum {0} min (< {1}) - nimic nou de adus" -f [int]$minDeLaUltima, $MinInterMinute)
            return
        }
    }

    # --- 1. Docker Desktop ---
    if (-not (Get-Process 'com.docker.backend' -ErrorAction SilentlyContinue)) {
        Scrie 'Docker Desktop nu ruleaza - il pornesc'
        Start-Process 'C:\Program Files\Docker\Docker\Docker Desktop.exe'
    }
    if (-not (Asteapta 'Docker gata' { (Tacut 'docker info') -eq 0 } 300)) {
        throw 'Docker nu a pornit'
    }

    # --- 2. Infra: DOAR Kafka + Redis din docker ---
    # PERICOL: `docker compose up -d` fara argumente porneste si `golstat-postgres`, mapat pe 0.0.0.0:5432,
    # adica exact portul pe care asculta deja Postgres-ul LOCAL (serviciul Windows) — acolo stau datele.
    # Cele doua asculta simultan si conexiunea aterizeaza nedeterminist: API-ul poate scrie intr-un
    # container GOL si colectarea zilei se pierde. De aceea pornim explicit doar kafka+redis si oprim
    # containerul de postgres daca cineva l-a pornit. (schema-registry nu e folosit de cod.)
    Scrie 'Pornesc Kafka + Redis'
    Push-Location $Radacina
    try {
        if ((Tacut 'docker compose up -d kafka redis') -ne 0) { throw 'docker compose up a esuat' }
    } finally { Pop-Location }

    if ((cmd /c "docker ps --filter name=golstat-postgres --format ""{{.Names}}""" ) -match 'golstat-postgres') {
        Scrie 'Containerul golstat-postgres ruleaza si umbreste Postgres-ul local pe 5432 - il opresc' 'ATENTIE'
        Tacut 'docker stop golstat-postgres' | Out-Null
    }

    # docker-compose.yml n-are healthcheck-uri, deci verificam noi fiecare serviciu.
    $infraOk = (Asteapta 'Redis' { (Tacut 'docker exec golstat-redis redis-cli ping') -eq 0 } 120) `
        -and (Asteapta 'Kafka' { (Tacut 'docker exec golstat-kafka kafka-broker-api-versions --bootstrap-server localhost:9092') -eq 0 } 180) `
        -and (Asteapta 'Postgres local' { (Tacut "`"$Psql`" -U postgres -h 127.0.0.1 -d golstat -c ""select 1""") -eq 0 } 120)
    if (-not $infraOk) { throw 'Infra nu a devenit disponibila' }

    # --- 3. Jar-e ---
    # NU construim daca jar-ele exista deja. Gradle ia un lock global pe ~/.gradle/caches; daca
    # IntelliJ e deschis (are propriul daemon), build-ul nostru se BLOCHEAZA la nesfarsit asteptand
    # lock-ul — masurat: 19 minute cu daemonul la 0% CPU, adica fiecare rulare programata ar fi ars
    # cele 2 ore de ExecutionTimeLimit fara sa colecteze nimic.
    # Deci: build doar la bootstrap (jar lipsa), si atunci cu --no-daemon si cu output in log.
    # Dupa o modificare de cod, reconstruieste manual: gradlew :api:bootJar :data-collector:bootJar
    $jarApi      = Join-Path $Radacina 'backend\api\build\libs\api-0.1.0.jar'
    $jarColector = Join-Path $Radacina 'backend\data-collector\build\libs\data-collector-0.1.0.jar'
    $lipsa = @($jarApi, $jarColector) | Where-Object { -not (Test-Path $_) }

    if ($lipsa) {
        Scrie "Lipsesc jar-ele ($($lipsa.Count)) - construiesc o data, cu --no-daemon"
        $logBuild = Join-Path $DirLoguri 'build.log'
        Push-Location $Radacina
        try {
            cmd /c ".\gradlew.bat :api:bootJar :data-collector:bootJar --no-daemon > `"$logBuild`" 2>&1"
            $codBuild = $LASTEXITCODE
        } finally { Pop-Location }
        if ($codBuild -ne 0) { throw "bootJar a esuat (cod $codBuild) - detalii in $logBuild" }
        foreach ($j in @($jarApi, $jarColector)) {
            if (-not (Test-Path $j)) { throw "Nu gasesc jar-ul $j (s-a schimbat versiunea in build.gradle?)" }
        }
    }

    # Vechimea jar-elor in log: daca ai schimbat cod si ai uitat sa reconstruiesti, se vede aici
    # de ce ruleaza tot comportamentul vechi. Nu construim automat (vezi lock-ul Gradle de mai sus),
    # dar spunem RASPICAT cand jar-ul e mai vechi decat sursele — altfel o corectura pare ca n-a
    # avut niciun efect, desi de fapt nici n-a ajuns in binarul care ruleaza.
    $sursa = Join-Path $Radacina 'backend'
    $celMaiNouCod = Get-ChildItem $sursa -Recurse -Include '*.java', '*.yml' -File -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -notmatch '\\build\\' } |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1

    foreach ($j in @($jarApi, $jarColector)) {
        $jar = Get-Item $j
        Scrie ("Jar {0} - construit {1:yyyy-MM-dd HH:mm}" -f $jar.Name, $jar.LastWriteTime)
        if ($celMaiNouCod -and $celMaiNouCod.LastWriteTime -gt $jar.LastWriteTime) {
            Scrie ("{0} e mai VECHI decat sursele (ex. {1} la {2:yyyy-MM-dd HH:mm}) - ruleaza cod invechit. Reconstruieste: gradlew.bat :api:bootJar :data-collector:bootJar" `
                -f $jar.Name, $celMaiNouCod.Name, $celMaiNouCod.LastWriteTime) 'ATENTIE'
        }
    }

    # --- 4. API (consumatorul Kafka -> Postgres) ---
    # Fara actuator in proiect - folosim un endpoint real ca dovada ca s-a ridicat contextul.
    $apiRaspunde = {
        try { (Invoke-WebRequest 'http://localhost:8080/api/v1/meciuri/live' -UseBasicParsing -TimeoutSec 5).StatusCode -eq 200 }
        catch { $false }
    }

    if (& $apiRaspunde) {
        # Ruleaza deja (tipic: pornit din IntelliJ). Il FOLOSIM si nu-l oprim la final — nu e al nostru.
        # Fara verificarea asta am fi pornit un al doilea proces care moare la bind pe 8080, in timp ce
        # health check-ul ar fi trecut pe instanta straina: ar fi mers accidental, imposibil de depanat.
        Scrie 'API-ul ruleaza deja pe 8080 - il folosesc ca atare (nu-l opresc la final)'
    } else {
        Scrie 'Pornesc API-ul in fundal'
        $procesApi = Start-Process -FilePath 'java' -ArgumentList '-jar', "`"$jarApi`"" `
            -WorkingDirectory $Radacina -PassThru -WindowStyle Hidden
        if (-not (Asteapta 'API gata' $apiRaspunde 300)) {
            throw 'API-ul nu a pornit'
        }
    }

    # --- 5. Colectorul (sincron; iese singur datorita modului one-shot) ---
    # Iesirea lui merge intr-un fisier propriu: pornit de Task Scheduler, stdout-ul se pierde, iar
    # in jurnal ramanea doar "cod 1" — fara stack trace, fara motiv (masurat 2026-07-21: a fost
    # nevoie de reconstituire din procese ca sa aflam ca era un conflict de port).
    # Numele incepe cu "colectare-" ca sa intre in rotatia de 14 zile de mai jos.
    $logColector = Join-Path $DirLoguri ("colectare-colector-{0}.log" -f (Get-Date -Format 'yyyy-MM-dd'))
    Scrie "Rulez colectorul in mod one-shot (iesirea completa: $(Split-Path $logColector -Leaf))"
    Push-Location $Radacina
    try {
        cmd /c "java -jar `"$jarColector`" --spring.profiles.active=oneshot >> `"$logColector`" 2>&1"
        $codColector = $LASTEXITCODE
    } finally { Pop-Location }
    if ($codColector -ne 0) {
        Scrie "Colectorul a iesit cu cod $codColector" 'EROARE'
        # Ultimele linii direct in jurnalul principal — acolo se uita omul intai.
        Get-Content $logColector -Tail 15 -ErrorAction SilentlyContinue |
            ForEach-Object { Scrie "  | $_" 'EROARE' }
        $cod = 1
    }

    # --- 6. Drenarea Kafka: API-ul trebuie sa apuce sa scrie tot in Postgres ---
    $lagZero = Asteapta 'Kafka drenat' {
        $iesire = cmd /c "docker exec golstat-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group golstat-api 2>nul"
        if ($LASTEXITCODE -ne 0) { return $false }
        $laguri = @($iesire | Select-String -Pattern '^\S+\s+\S+\s+\d+\s+\S+\s+\S+\s+(\d+)' |
                    ForEach-Object { [int]$_.Matches[0].Groups[1].Value })
        # fara randuri = grupul n-a raportat inca; asteptam, nu declaram succes
        return ($laguri.Count -gt 0 -and ($laguri | Measure-Object -Sum).Sum -eq 0)
    } 600
    if (-not $lagZero) { Scrie 'Kafka nu s-a drenat complet - restul se ingereaza la rularea urmatoare' 'ATENTIE' }

    # --- 7. Consum cota ---
    $cheieCota = "golstat:af:quota:{0}" -f (Get-Date).ToUniversalTime().ToString('yyyy-MM-dd')
    $consum = cmd /c "docker exec golstat-redis redis-cli get $cheieCota 2>nul"
    Scrie "Requesturi API-Football consumate azi: $consum"

    $total = & $Psql -U postgres -h 127.0.0.1 -d golstat -A -t -c 'select count(*) from fixture;'
    Scrie "Meciuri in baza locala: $($total -join '')"

    $gata = cmd /c "docker exec golstat-redis redis-cli keys ""golstat:backfill:*"" 2>nul"
    Scrie "Tinte de backfill terminate: $(@($gata).Count)"

    # Marcaj pentru anti-rulari-dese (vezi $MinInterMinute). Scris DOAR dupa un ciclu care a ajuns aici,
    # deci un ciclu cazut la mijloc nu blocheaza reincercarea.
    [System.IO.File]::WriteAllText($FisierUlt, (Get-Date).ToString('o'))
}
catch {
    Scrie $_.Exception.Message 'EROARE'
    $cod = 1
}
finally {
    if ($procesApi -and -not $procesApi.HasExited) {
        Scrie 'Opresc API-ul'
        Tacut "taskkill /PID $($procesApi.Id) /T /F" | Out-Null
    }

    # Daca am sarit fiindca ruleaza alt ciclu, PID-ul din fisier e AL LUI — sters de noi, watcher-ul
    # de adormire ar crede ca s-a terminat colectarea si ar suspenda laptopul in mijlocul ei.
    if (-not $altCiclu) {
        Remove-Item $FisierPid -Force -ErrorAction SilentlyContinue
    }

    # Rotatie loguri: pastram 14 zile
    Get-ChildItem $DirLoguri -Filter 'colectare-*.log' -ErrorAction SilentlyContinue |
        Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-14) } |
        Remove-Item -Force -ErrorAction SilentlyContinue

    Scrie "=== Ciclu terminat (cod $cod) ==="

    if ($Adoarme) {
        $idle = [math]::Round([GolstatIdle]::MinuteIdle(), 1)
        $trezitDeNoi = TrezitRecent $MinuteDeLaTrezire

        if ($altCiclu) {
            # Am sarit fiindca alt ciclu colecteaza CHIAR ACUM. Adormirea l-ar taia in mijloc: task-ul
            # ne-a trezit (deci $trezitDeNoi e adevarat) si nimeni nu e la tastatura (deci idle e mare),
            # adica exact conditiile in care am fi suspendat laptopul peste o colectare in plina desfasurare.
            Scrie 'Alt ciclu colecteaza - NU adorm laptopul (adoarme el la final)'
        } elseif (-not $trezitDeNoi) {
            Scrie 'Laptopul era deja pornit - il las asa (respectam planul de alimentare)'
        } elseif ($idle -lt $PragIdleMinute) {
            Scrie "Activitate acum $idle min - NU adorm laptopul"
        } else {
            Scrie "Trezit de task, inactiv de $idle min - revin in sleep"
            # Hibernarea e dezactivata pe masina asta (verificat cu `powercfg /a`), deci asta face
            # sleep S3 real. Daca activezi vreodata hibernarea, ruleaza `powercfg -h off`.
            Start-Sleep -Seconds 2
            rundll32.exe powrprof.dll,SetSuspendState 0,1,0
        }
    }
}

exit $cod
