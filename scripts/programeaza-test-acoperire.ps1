<#
.SYNOPSIS
    Programeaza testul de acoperire sa ruleze O SINGURA DATA, dupa resetul de cota.

.DESCRIPTION
    Cota API-Football se reseteaza la miezul noptii UTC = 03:00 ora Romaniei (02:00 iarna).
    Rulam la 03:25 — dupa ciclul de colectare de la 03:10, ca sa nu ne batem pe cota cu el si ca
    testul sa prinda oricum cei ~32 de requesturi de care are nevoie.

    Task-ul se autodistruge dupa rulare (`-DeleteExpiredTaskAfter`), fiindca e o masuratoare unica.
    Iesirea merge in scripts/logs/acoperire-<data>.log.

.PARAMETER Dezinstaleaza
    Sterge task-ul in loc sa-l creeze.
#>
[CmdletBinding()]
param([switch] $Dezinstaleaza)

$ErrorActionPreference = 'Stop'
$NumeTask = 'golstat-test-acoperire'

if ($Dezinstaleaza) {
    Unregister-ScheduledTask -TaskName $NumeTask -Confirm:$false -ErrorAction SilentlyContinue
    Write-Host "Task-ul '$NumeTask' a fost sters."
    return
}

$scriptAcoperire = Join-Path $PSScriptRoot 'testeaza-acoperire.ps1'
$scriptSezoane   = Join-Path $PSScriptRoot 'masoara-sezoane-jucatori.ps1'
foreach ($s in @($scriptAcoperire, $scriptSezoane)) {
    if (-not (Test-Path $s)) { throw "Nu gasesc $s" }
}

# Ambele masuratori in acelasi log, una dupa alta (~32 + ~40 requesturi).
# Data se evalueaza LA RULARE, nu acum — de aceea backtick pe $(Get-Date).
$log = "$PSScriptRoot\logs\acoperire-`$(Get-Date -Format yyyy-MM-dd).log"
$comanda = "& '$scriptAcoperire' *> '$log'; & '$scriptSezoane' *>> '$log'"

$actiune = New-ScheduledTaskAction -Execute 'powershell.exe' `
    -Argument "-NoProfile -ExecutionPolicy Bypass -Command `"$comanda`"" `
    -WorkingDirectory $PSScriptRoot

# Maine la 03:25. Daca laptopul doarme si nu se trezeste, StartWhenAvailable il ruleaza la prima
# ocazie (dimineata, la deblocare) — testul nu e sensibil la ora, doar la cota resetata.
$cand = (Get-Date).Date.AddDays(1).AddHours(3).AddMinutes(25)
$trigger = New-ScheduledTaskTrigger -Once -At $cand
# `-DeleteExpiredTaskAfter` cere un EndBoundary pe trigger, altfel Register-ScheduledTask respinge
# XML-ul cu "missing a required element". 24h e destul: cu StartWhenAvailable, un laptop adormit
# ruleaza testul la prima deblocare din interval.
$trigger.EndBoundary = $cand.AddHours(24).ToString('s')

$setari = New-ScheduledTaskSettingsSet -WakeToRun -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
    -StartWhenAvailable -ExecutionTimeLimit (New-TimeSpan -Minutes 30) `
    -DeleteExpiredTaskAfter (New-TimeSpan -Days 2)

Register-ScheduledTask -TaskName $NumeTask -Action $actiune -Trigger $trigger -Settings $setari `
    -Description 'Sondeaza acoperirea de statistici pentru ligile candidate (o singura data).' `
    -User $env:USERNAME -Force | Out-Null

Write-Host "Task '$NumeTask' programat pentru $cand."
Write-Host "Rezultatul va fi in: $PSScriptRoot\logs\acoperire-<data>.log"
