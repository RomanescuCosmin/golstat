<#
.SYNOPSIS
    Un ciclu complet de colectare golstat: infra -> API -> colector -> drenare Kafka -> oprire.

.DESCRIPTION
    Gandit pentru Windows Task Scheduler cu wake timers: PC-ul se trezeste din sleep, ruleaza acest
    script si (cu -Adoarme) revine in sleep. API-ul trebuie sa fie pornit in acelasi interval pentru
    ca el e cel care consuma din Kafka si scrie in Postgres — colectorul doar publica.

.PARAMETER Adoarme
    Reintra in sleep la final. Il pune task-ul programat; la rulare manuala il omiti.
    Nu adoarme daca esti la tastatura (idle sub -PragIdleMinute) — nu vrem sa stingem PC-ul sub mana ta.

.PARAMETER PragIdleMinute
    Cate minute de inactivitate sunt necesare ca -Adoarme sa aiba efect. Implicit 10.

.EXAMPLE
    .\colectare-oneshot.ps1
    .\colectare-oneshot.ps1 -Adoarme
#>
[CmdletBinding()]
param(
    [switch] $Adoarme,
    [int]    $PragIdleMinute = 10
)

# Intentionat 'Continue', nu 'Stop': scriptul ruleaza multe executabile native (docker, gradlew) care
# scriu pe stderr in mod normal. In PowerShell 5.1, stderr de la un .exe devine ErrorRecord si cu
# 'Stop' ar arunca desi comanda a reusit. Verificam explicit $LASTEXITCODE si aruncam noi cu `throw`.
$ErrorActionPreference = 'Continue'

$Radacina  = Split-Path -Parent $PSScriptRoot
$Psql      = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
$DirLoguri = Join-Path $PSScriptRoot 'logs'
$Log       = Join-Path $DirLoguri ("colectare-{0}.log" -f (Get-Date -Format 'yyyy-MM-dd'))

if (-not (Test-Path $DirLoguri)) { New-Item -ItemType Directory -Path $DirLoguri | Out-Null }

function Scrie($mesaj, $nivel = 'INFO') {
    $linie = "{0} [{1}] {2}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $nivel, $mesaj
    Write-Host $linie
    Add-Content -Path $Log -Value $linie -Encoding utf8
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

$procesApi   = $null
$cod         = 0
$codColector = 0

try {
    Scrie "=== Pornire ciclu (radacina: $Radacina) ==="

    if (-not $env:API_FOOTBALL_KEY) {
        throw "API_FOOTBALL_KEY nu e setat. Seteaza-l ca variabila de mediu de UTILIZATOR (nu in script)."
    }
    # doar pentru verificarea de sanatate a bazei locale de dezvoltare
    if (-not $env:PGPASSWORD) { $env:PGPASSWORD = 'root' }

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
    # `bootRun` reporneste Gradle si recompileaza de fiecare data (~zeci de secunde in plus, cu PC-ul
    # treaz degeaba). Construim o data si rulam `java -jar`; build-ul e incremental, deci aproape gratis
    # cand nu s-a schimbat nimic.
    Scrie 'Construiesc jar-ele (incremental)'
    Push-Location $Radacina
    try {
        if ((Tacut '.\gradlew.bat :api:bootJar :data-collector:bootJar') -ne 0) { throw 'bootJar a esuat' }
    } finally { Pop-Location }

    $jarApi      = Join-Path $Radacina 'backend\api\build\libs\api-0.1.0.jar'
    $jarColector = Join-Path $Radacina 'backend\data-collector\build\libs\data-collector-0.1.0.jar'
    foreach ($j in @($jarApi, $jarColector)) {
        if (-not (Test-Path $j)) { throw "Nu gasesc jar-ul $j (s-a schimbat versiunea in build.gradle?)" }
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
    Scrie 'Rulez colectorul in mod one-shot'
    Push-Location $Radacina
    try {
        & java -jar $jarColector '--spring.profiles.active=oneshot'
        $codColector = $LASTEXITCODE
    } finally { Pop-Location }
    if ($codColector -ne 0) { Scrie "Colectorul a iesit cu cod $codColector" 'EROARE'; $cod = 1 }

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

    # Rotatie loguri: pastram 14 zile
    Get-ChildItem $DirLoguri -Filter 'colectare-*.log' -ErrorAction SilentlyContinue |
        Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-14) } |
        Remove-Item -Force -ErrorAction SilentlyContinue

    Scrie "=== Ciclu terminat (cod $cod) ==="

    if ($Adoarme) {
        $idle = [math]::Round([GolstatIdle]::MinuteIdle(), 1)
        if ($idle -ge $PragIdleMinute) {
            Scrie "Inactiv de $idle min - revin in sleep"
            # Hibernarea e dezactivata pe masina asta (verificat cu `powercfg /a`), deci asta face
            # sleep S3 real. Daca activezi vreodata hibernarea, ruleaza `powercfg -h off`.
            Start-Sleep -Seconds 2
            rundll32.exe powrprof.dll,SetSuspendState 0,1,0
        } else {
            Scrie "Activitate acum $idle min - NU adorm PC-ul"
        }
    }
}

exit $cod
