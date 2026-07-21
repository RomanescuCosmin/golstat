<#
.SYNOPSIS
    Inregistreaza task-ul Windows care trezeste PC-ul si ruleaza colectarea golstat.

.DESCRIPTION
    Ruleaza o singura data. Creeaza un task care porneste la fiecare -LaOre ore, cu "wake the computer
    to run this task" pornit, si care ii cere scriptului sa readoarma PC-ul.

    NU are nevoie de Administrator daca wake timers sunt deja activate in power plan (cazul obisnuit) —
    task-ul ruleaza cu drepturile tale normale. Daca nu sunt, scriptul iti spune ce sa rulezi elevat.

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
# Verificam intai — daca e deja pornit (cazul obisnuit), nu avem nevoie de Administrator deloc.
$scheme = (powercfg /getactivescheme) -replace '.*GUID: ([a-f0-9-]+).*', '$1'
$rtc = powercfg /query $scheme SUB_SLEEP RTCWAKE
$acPornit = ($rtc | Select-String 'Current AC Power Setting Index:\s+0x00000001').Count -gt 0
$dcPornit = ($rtc | Select-String 'Current DC Power Setting Index:\s+0x00000001').Count -gt 0

if ($acPornit -and $dcPornit) {
    Write-Host 'Wake timers: deja activate (priza + baterie).'
} elseif ($eAdmin) {
    powercfg /setacvalueindex SCHEME_CURRENT SUB_SLEEP RTCWAKE 1
    powercfg /setdcvalueindex SCHEME_CURRENT SUB_SLEEP RTCWAKE 1
    powercfg /setactive SCHEME_CURRENT
    Write-Host 'Wake timers: activate acum (priza + baterie).'
} else {
    Write-Warning "Wake timers NU sunt activate (AC=$acPornit, baterie=$dcPornit) si nu sunt Administrator."
    Write-Warning 'Task-ul se inregistreaza, dar NU va trezi laptopul din sleep pana nu rulezi, ca Administrator:'
    Write-Warning '  powercfg /setacvalueindex SCHEME_CURRENT SUB_SLEEP RTCWAKE 1'
    Write-Warning '  powercfg /setdcvalueindex SCHEME_CURRENT SUB_SLEEP RTCWAKE 1'
    Write-Warning '  powercfg /setactive SCHEME_CURRENT'
}

$actiune = New-ScheduledTaskAction -Execute 'powershell.exe' `
    -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$scriptColectare`" -Adoarme" `
    -WorkingDirectory $PSScriptRoot

# Repetitie pe 24h, decalata cu 10 minute fata de ora fixa.
# De ce nu la fix: cota API-Football se reseteaza la miezul noptii UTC = 03:00 ora Romaniei vara
# (02:00 iarna). Cu pornire la 00:00 si interval de 3h, o rulare ar cadea exact pe granita resetului.
# Decalajul o muta sigur DUPA reset, deci ciclul de noapte prinde cota proaspata intreaga.
# 10 ani, nu [TimeSpan]::MaxValue: acesta din urma serializeaza ca P99999999DT23H59M59S, pe care
# Task Scheduler il respinge cu "value incorrectly formatted or out of range".
$triggerRepetat = New-ScheduledTaskTrigger -Once -At (Get-Date).Date.AddMinutes(10) `
    -RepetitionInterval (New-TimeSpan -Hours $LaOre) `
    -RepetitionDuration (New-TimeSpan -Days 3650)

# Trigger pe DEBLOCARE: cel mai fiabil moment de colectare. Trezirea programata din sleep e
# nesigura pe hardware-ul asta (verificat: 18:09/21:09 s-au trezit, 00:10/03:10/06:10 nu, desi wake
# timers erau permise si sesiunea logata). Deblocarea insa e garantata cand te apuci de lucru
# dimineata, iar fereastra de 3 zile + largirea automata prind tot ce s-a ratat peste noapte.
# Cmdlet-ul New-ScheduledTaskTrigger nu expune session-state-change, deci il construim prin CIM.
# StateChange = 8 => TASK_SESSION_UNLOCK.
$clasaTrigger = Get-CimClass -Namespace 'Root/Microsoft/Windows/TaskScheduler' `
    -ClassName 'MSFT_TaskSessionStateChangeTrigger'
$triggerDeblocare = New-CimInstance -CimClass $clasaTrigger -ClientOnly
$triggerDeblocare.StateChange = 8
$triggerDeblocare.UserId      = "$env:USERDOMAIN\$env:USERNAME"
$triggerDeblocare.Enabled     = $true
# Mica intarziere: lasa reteaua/Docker sa se aseze dupa resume; anti-rulari-dese din script taie
# oricum unlock-urile repetate (nu ruleaza daca un ciclu s-a incheiat in ultimele 30 min).
$triggerDeblocare.Delay       = 'PT1M'

$trigger = @($triggerRepetat, $triggerDeblocare)

$setari = New-ScheduledTaskSettingsSet `
    -WakeToRun `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -StartWhenAvailable `
    -ExecutionTimeLimit (New-TimeSpan -Hours 2) `
    -MultipleInstances IgnoreNew

# Fara -RunLevel Highest: scriptul nu are nevoie de elevare (docker, psql, java, taskkill pe propriul
# proces si SetSuspendState merg toate cu drepturi normale). Asa il poate inregistra si un cont fara
# drepturi de Administrator, si rulam cu privilegiul minim necesar.
Register-ScheduledTask -TaskName $NumeTask -Action $actiune -Trigger $trigger -Settings $setari `
    -Description 'Colecteaza date API-Football pentru golstat; trezeste PC-ul din sleep.' `
    -User $env:USERNAME -Force | Out-Null

Write-Host "Task-ul '$NumeTask' a fost inregistrat (la fiecare $LaOre ore + la deblocare, cu trezire din sleep)."
Write-Host ''
Write-Host 'Verificari utile:'
Write-Host '  Get-ScheduledTask golstat-colectare | Get-ScheduledTaskInfo'
Write-Host '  powercfg -waketimers'
Write-Host '  Start-ScheduledTask golstat-colectare      # test imediat'
Write-Host "  Get-Content '$PSScriptRoot\logs\colectare-$(Get-Date -Format yyyy-MM-dd).log' -Tail 30"
