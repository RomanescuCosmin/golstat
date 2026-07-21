<#
.SYNOPSIS
    Asteapta terminarea unei colectari deja pornite, apoi adoarme laptopul.

.DESCRIPTION
    Pentru cazul "plec de la birou, dar colectarea inca ruleaza": nu vrem nici s-o intrerupem,
    nici sa lasam laptopul pornit toata noaptea.

    Spre deosebire de -Adoarme din colectare-oneshot.ps1 (care adoarme DOAR daca task-ul a trezit
    laptopul), asta adoarme neconditionat — a cerut-o omul explicit inainte sa plece.

.PARAMETER MaxMinute
    Plafon de asteptare. Daca colectarea se blocheaza, adormim oricum la expirare — altfel laptopul
    ar ramane pornit la nesfarsit, exact ce voiam sa evitam.
#>
[CmdletBinding()]
param(
    [int] $MaxMinute = 120
)

$ErrorActionPreference = 'Continue'

$DirLoguri = Join-Path $PSScriptRoot 'logs'
$Log       = Join-Path $DirLoguri ("colectare-{0}.log" -f (Get-Date -Format 'yyyy-MM-dd'))
$FisierPid = Join-Path $DirLoguri '.colectare.pid'
$eu        = $PID

function Scrie($mesaj) {
    $linie = "{0} [SLEEP] {1}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $mesaj
    Write-Host $linie
    Add-Content -Path $Log -Value $linie -Encoding utf8
}

# Colectarea inca ruleaza?
# Sursa de adevar e fisierul PID scris de colectare-oneshot.ps1, NU cautarea dupa linia de comanda:
# aceea dadea fals pozitiv la orice comanda care continea numele scriptului (inclusiv un diagnostic
# tastat in consola), iar watcher-ul ar fi asteptat degeaba pana la plafon.
# Verificam si ca procesul chiar traieste — un PID ramas de la o rulare cazuta nu ne blocheaza.
function ColectareaRuleaza {
    if (-not (Test-Path $FisierPid)) { return $false }
    try {
        $pidColectare = [int](Get-Content $FisierPid -Raw).Trim()
    } catch { return $false }
    if ($pidColectare -eq $eu) { return $false }
    return $null -ne (Get-Process -Id $pidColectare -ErrorAction SilentlyContinue)
}

Scrie "Astept terminarea colectarii, apoi adorm laptopul (plafon $MaxMinute min)"

$limita = (Get-Date).AddMinutes($MaxMinute)
$vazutaPornita = $false

while ((Get-Date) -lt $limita) {
    if (ColectareaRuleaza) {
        $vazutaPornita = $true
    } elseif ($vazutaPornita) {
        Scrie 'Colectarea s-a terminat'
        break
    } else {
        # Nu apucasem s-o vedem pornita: fie s-a terminat deja, fie n-a pornit niciodata.
        Scrie 'Nicio colectare in desfasurare'
        break
    }
    Start-Sleep -Seconds 15
}

if ((Get-Date) -ge $limita) {
    Scrie "Plafon de $MaxMinute min atins - adorm oricum"
}

# Lasam ingestia sa se aseze si logurile sa se scrie inainte de suspendare.
Start-Sleep -Seconds 10
Scrie 'Adorm laptopul acum'
# Hibernarea e dezactivata pe masina asta (powercfg /a), deci asta face sleep S3 real.
rundll32.exe powrprof.dll,SetSuspendState 0,1,0
