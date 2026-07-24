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

# Cate un trigger ZILNIC per ora de rulare, nu unul `-Once` cu repetitie.
# De ce: masurat pe 2026-07-21, un `-Once` + `RepetitionInterval 3h` a trezit laptopul la 18:10 si
# 21:10, apoi NU l-a mai trezit la 00:10, 03:10 si 06:10 — desi wake timers erau permise (AC+DC),
# masina suporta S3 clasic si sesiunea era logata. Windows armeaza ceasul RTC pentru URMATOAREA
# ocurenta a unui trigger; pe repetitii, arm-area se pierde peste granita de zi. Un DailyTrigger la
# ora fixa are propria ocurenta pentru fiecare zi, deci se re-armeaza singur.
#
# Decalajul de 10 minute fata de ora fixa: cota API-Football se reseteaza la miezul noptii UTC =
# 03:00 ora Romaniei vara (02:00 iarna). O rulare exact la 03:00 ar cadea pe granita resetului.
$oreRulare = 0..23 | Where-Object { $_ % $LaOre -eq 0 }
$triggereZilnice = $oreRulare | ForEach-Object {
    New-ScheduledTaskTrigger -Daily -At (Get-Date).Date.AddHours($_).AddMinutes(10)
}

# Trigger pe DEBLOCARE: plasa de siguranta peste trezirea programata. Chiar daca un wake ratat lasa
# o gaura peste noapte, deblocarea de dimineata declanseaza ciclul, iar fereastra de 3 zile plus
# largirea automata din UltimaRulare recupereaza tot ce s-a pierdut.
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

$trigger = @($triggereZilnice) + $triggerDeblocare

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

Write-Host ("Task-ul '{0}' a fost inregistrat: {1} triggere zilnice ({2}) + deblocare, cu trezire din sleep." -f `
    $NumeTask, $triggereZilnice.Count, (($oreRulare | ForEach-Object { '{0:00}:10' -f $_ }) -join ', '))
Write-Host ''
Write-Host 'Verificari utile:'
Write-Host '  Get-ScheduledTask golstat-colectare | Get-ScheduledTaskInfo'
Write-Host '  powercfg -waketimers'
Write-Host '  Start-ScheduledTask golstat-colectare      # test imediat'
Write-Host "  Get-Content '$PSScriptRoot\logs\colectare-$(Get-Date -Format yyyy-MM-dd).log' -Tail 30"
