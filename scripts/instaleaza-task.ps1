<#
.SYNOPSIS
    Inregistreaza task-ul Windows care trezeste PC-ul si ruleaza colectarea golstat.

.DESCRIPTION
    Ruleaza ca ADMINISTRATOR (o singura data). Creeaza un task care porneste la fiecare -LaOre ore,
    cu "wake the computer to run this task" pornit, si care ii cere scriptului sa readoarma PC-ul.

    Masina asta suporta Standby (S3) clasic — verificat cu `powercfg /a` — deci wake timers
    functioneaza fiabil. Scriptul verifica si activeaza si setarea de power plan "Allow wake timers",
    care altfel anuleaza tacut trigger-ul.

.PARAMETER LaOre
    Intervalul intre rulari. Implicit 3 — un ciclu (Docker + Gradle + colectare) dureaza cateva
    minute, deci la 1 ora PC-ul aproape ca n-ar mai dormi.

.PARAMETER Dezinstaleaza
    Sterge task-ul in loc sa-l creeze.

.EXAMPLE
    .\instaleaza-task.ps1
    .\instaleaza-task.ps1 -LaOre 6
    .\instaleaza-task.ps1 -Dezinstaleaza
#>
[CmdletBinding()]
param(
    [int]    $LaOre = 3,
    [switch] $Dezinstaleaza
)

$ErrorActionPreference = 'Stop'
$NumeTask = 'golstat-colectare'

$eAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()
          ).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $eAdmin) { throw 'Ruleaza acest script ca Administrator.' }

if ($Dezinstaleaza) {
    Unregister-ScheduledTask -TaskName $NumeTask -Confirm:$false
    Write-Host "Task-ul '$NumeTask' a fost sters."
    return
}

$scriptColectare = Join-Path $PSScriptRoot 'colectare-oneshot.ps1'
if (-not (Test-Path $scriptColectare)) { throw "Nu gasesc $scriptColectare" }

if (-not $env:API_FOOTBALL_KEY) {
    Write-Warning 'API_FOOTBALL_KEY nu e vizibil in sesiunea curenta.'
    Write-Warning 'Task-ul ruleaza sub contul tau, deci seteaz-o ca variabila de mediu de UTILIZATOR:'
    Write-Warning '  [Environment]::SetEnvironmentVariable("API_FOOTBALL_KEY", "<cheia>", "User")'
}

# "Allow wake timers" din power plan: daca e Disable, trigger-ul cu -WakeToRun nu trezeste nimic.
powercfg /setacvalueindex SCHEME_CURRENT SUB_SLEEP RTCWAKE 1
powercfg /setdcvalueindex SCHEME_CURRENT SUB_SLEEP RTCWAKE 1
powercfg /setactive SCHEME_CURRENT
Write-Host 'Wake timers activate in power plan (AC + baterie).'

$actiune = New-ScheduledTaskAction -Execute 'powershell.exe' `
    -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$scriptColectare`" -Adoarme" `
    -WorkingDirectory $PSScriptRoot

# Repetitie pe 24h pornind de la miezul noptii: acopera si zilele cand PC-ul sta inchis.
$trigger = New-ScheduledTaskTrigger -Once -At (Get-Date).Date `
    -RepetitionInterval (New-TimeSpan -Hours $LaOre) `
    -RepetitionDuration ([TimeSpan]::MaxValue)

$setari = New-ScheduledTaskSettingsSet `
    -WakeToRun `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -StartWhenAvailable `
    -ExecutionTimeLimit (New-TimeSpan -Hours 2) `
    -MultipleInstances IgnoreNew

Register-ScheduledTask -TaskName $NumeTask -Action $actiune -Trigger $trigger -Settings $setari `
    -Description 'Colecteaza date API-Football pentru golstat; trezeste PC-ul din sleep.' `
    -RunLevel Highest -User $env:USERNAME -Force | Out-Null

Write-Host "Task-ul '$NumeTask' a fost inregistrat (la fiecare $LaOre ore, cu trezire din sleep)."
Write-Host ''
Write-Host 'Verificari utile:'
Write-Host '  Get-ScheduledTask golstat-colectare | Get-ScheduledTaskInfo'
Write-Host '  powercfg -waketimers'
Write-Host '  Start-ScheduledTask golstat-colectare      # test imediat'
Write-Host "  Get-Content '$PSScriptRoot\logs\colectare-$(Get-Date -Format yyyy-MM-dd).log' -Tail 30"
