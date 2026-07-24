<#
.SYNOPSIS
    Masoara cate sezoane are in medie un jucator la API-Football, ca sa stim cat costa aducerea
    intregii cariere pentru toti jucatorii (Faza 3).

.DESCRIPTION
    Costul Fazei 3 = pentru fiecare jucator, 1 request `/players/seasons` + cate 1 request per sezon.
    Necunoscuta e „cate sezoane per jucator". O estimam pe un esantion ALEATOR din baza locala —
    aleator, nu ales pe sprinceana: vedetele au cariere lungi in API, juniorii au 1-2 sezoane, iar
    un esantion partinitor ar deplasa estimarea cu saptamani.

.PARAMETER Esantion
    Cati jucatori sondam. Implicit 40 (= 40 requesturi). Peste ~30 estimarea se stabilizeaza.
#>
[CmdletBinding()]
param([int] $Esantion = 40)

$ErrorActionPreference = 'Continue'

if (-not $env:API_FOOTBALL_KEY) { throw 'API_FOOTBALL_KEY nu e setat.' }
$Antet = @{ 'x-apisports-key' = $env:API_FOOTBALL_KEY }
$Psql  = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
if (-not $env:PGPASSWORD) { $env:PGPASSWORD = 'root' }

Write-Host "=== Esantion aleator de $Esantion jucatori din baza locala ==="
# `select distinct ... order by random()` e refuzat de Postgres (expresia de sortare nu e in select
# list), de aceea filtram cu IN in loc de join+distinct.
$ids = & $Psql -U postgres -h 127.0.0.1 -d golstat -A -t -c @"
select id from player
where id in (select player_id from player_season_stats)
order by random() limit $Esantion;
"@
$ids = @($ids | ForEach-Object { $_.Trim() } | Where-Object { $_ -match '^\d+$' })
Write-Host "Am primit $($ids.Count) id-uri."

$masuratori = @()
$consum = 0

foreach ($id in $ids) {
    try {
        $r = Invoke-RestMethod -Uri "https://v3.football.api-sports.io/players/seasons?player=$id" `
             -Headers $Antet -TimeoutSec 30
        $consum++
        $n = if ($r.response) { @($r.response).Count } else { 0 }
        $masuratori += $n
        Write-Host ("  jucator {0,-8} -> {1} sezoane" -f $id, $n)
    } catch {
        Write-Warning "  jucator $id : $($_.Exception.Message)"
    }
    Start-Sleep -Milliseconds 250   # sub limita pe minut
}

if ($masuratori.Count -eq 0) { Write-Host 'Nicio masuratoare - probabil cota epuizata.'; return }

$stat = $masuratori | Measure-Object -Average -Minimum -Maximum
$sortate = $masuratori | Sort-Object
$median = $sortate[[int]($sortate.Count / 2)]

# Cati jucatori vom avea de procesat in total (inclusiv ligile adaugate, care nu au inca loturi aduse)
$totalJucatori = [int](& $Psql -U postgres -h 127.0.0.1 -d golstat -A -t -c 'select count(*) from player;')

Write-Host "`n=================== REZULTAT ==================="
Write-Host ("Esantion            : {0} jucatori" -f $masuratori.Count)
Write-Host ("Sezoane per jucator : medie {0:N1} | median {1} | min {2} | max {3}" -f `
    $stat.Average, $median, $stat.Minimum, $stat.Maximum)
Write-Host ("Jucatori in baza    : {0}" -f $totalJucatori)

# 1 request /players/seasons + cate 1 per sezon
$perJucator = 1 + $stat.Average
$totalReq = [math]::Round($totalJucatori * $perJucator)
Write-Host ("Cost per jucator    : {0:N1} requesturi (1 + sezoane)" -f $perJucator)
Write-Host ("TOTAL pentru baza actuala : {0:N0} requesturi" -f $totalReq)

foreach ($peZi in 4600, 3500) {
    Write-Host ("  la {0}/zi disponibili -> {1:N0} zile" -f $peZi, [math]::Ceiling($totalReq / $peZi))
}
Write-Host "`nRequesturi consumate de masuratoare: $consum"
