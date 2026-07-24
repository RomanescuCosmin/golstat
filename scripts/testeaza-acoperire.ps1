<#
.SYNOPSIS
    Verifica daca API-Football chiar are cornere/faulturi/cartonase pentru niste ligi candidate.

.DESCRIPTION
    De ce exista: o liga fara `fixture_team_stats` la furnizor iti da DOAR scoruri, deci piețele
    aplicatiei (cornere, faulturi, cartonase) raman goale — dar colectarea ei costa la fel de mult
    (~1200 requesturi pentru un sezon). Testul asta costa ~2 requesturi per liga si spune sigur
    care merita.

    NU ne luam dupa `coverage.statistics_fixtures` din /leagues: s-a dovedit mincinos pe sezoane noi
    (vezi memoria proiectului). Sondam un meci real si ne uitam ce tipuri de statistici vin.

.PARAMETER Ligi
    Id-uri de ligi de testat. Implicit: cei 16 candidati analizati pe 2026-07-21.

.PARAMETER Meciuri
    Cate meciuri sondam per liga, intinse uniform peste sezon. Un singur meci NU e o dovada:
    furnizorul are gauri punctuale (masurat la noi: 2 din 7234 de meciuri terminate n-au statistici
    desi liga le are). Cu 3+ meciuri distingem „liga n-are date" de „meciul ala n-are date".

.EXAMPLE
    .\testeaza-acoperire.ps1
    .\testeaza-acoperire.ps1 -Ligi 172,318,373,244,357,164 -Meciuri 5
#>
[CmdletBinding()]
param(
    [int[]] $Ligi,
    [int]   $Meciuri = 3
)

$ErrorActionPreference = 'Continue'

if (-not $env:API_FOOTBALL_KEY) { throw 'API_FOOTBALL_KEY nu e setat.' }
$Antet = @{ 'x-apisports-key' = $env:API_FOOTBALL_KEY }
$Baza  = 'https://v3.football.api-sports.io'

# Candidatii: prima liga a fiecarei tari. Sezonul preferat difera — ligile pe an calendaristic
# (Finlanda, Irlanda, Islanda) au sezonul 2026 in plina desfasurare, restul au 2025 incheiat.
$CANDIDATI = @(
    @{ id = 106; nume = 'Ekstraklasa';      tara = 'Polonia' }
    @{ id = 207; nume = 'Super League';     tara = 'Elvetia' }
    @{ id = 179; nume = 'Premiership';      tara = 'Scotia' }
    @{ id = 345; nume = 'Czech Liga';       tara = 'Cehia' }
    @{ id = 210; nume = 'HNL';              tara = 'Croatia' }
    @{ id = 271; nume = 'NB I';             tara = 'Ungaria' }
    @{ id = 172; nume = 'First League';     tara = 'Bulgaria' }
    @{ id = 286; nume = 'Super Liga';       tara = 'Serbia' }
    @{ id = 332; nume = 'Super Liga';       tara = 'Slovacia' }
    @{ id = 333; nume = 'Premier League';   tara = 'Ucraina' }
    @{ id = 383; nume = "Ligat Ha'al";      tara = 'Israel' }
    @{ id = 318; nume = '1. Division';      tara = 'Cipru' }
    @{ id = 373; nume = '1. SNL';           tara = 'Slovenia' }
    @{ id = 244; nume = 'Veikkausliiga';    tara = 'Finlanda' }
    @{ id = 357; nume = 'Premier Division'; tara = 'Irlanda' }
    @{ id = 164; nume = 'Urvalsdeild';      tara = 'Islanda' }
)

$deTestat = if ($Ligi) { $CANDIDATI | Where-Object { $Ligi -contains $_.id } } else { $CANDIDATI }

# Reincearca: un esec tranzitoriu returna $null, iar apelantul il trata identic cu "liga n-are date"
# si o clasa drept netestabila. Masurat pe 2026-07-22: Croatia, Slovacia si Ucraina au fost raportate
# "fara meciuri terminate" desi aveau sezoane complete de 180-243 de meciuri.
function Cere($cale) {
    for ($i = 1; $i -le 3; $i++) {
        try { return Invoke-RestMethod -Uri "$Baza/$cale" -Headers $Antet -TimeoutSec 30 }
        catch {
            Write-Warning "  request esuat ($cale), incercarea $i/3: $($_.Exception.Message)"
            Start-Sleep -Seconds $i
        }
    }
    return $null
}

$rezultate = @()
$consum = 0

foreach ($c in $deTestat) {
    Write-Host ("[{0,-4}] {1} ({2})" -f $c.id, $c.nume, $c.tara)

    # 1. meciuri TERMINATE: incercam sezonul incheiat, apoi cel curent (ligi pe an calendaristic)
    $terminate = @()
    $sezonGasit = $null
    $eroareRetea = $false
    foreach ($sezon in 2025, 2026) {
        $f = Cere "fixtures?league=$($c.id)&season=$sezon"
        $consum++
        if (-not $f) { $eroareRetea = $true; continue }
        if ($f.results -eq 0) { continue }
        $t = @($f.response | Where-Object { $_.fixture.status.short -in 'FT', 'AET', 'PEN' })
        if ($t.Count -gt 0) { $terminate = $t; $sezonGasit = $sezon; break }
    }

    if ($terminate.Count -eq 0) {
        # „Furnizorul n-are meciuri" si „nu l-am putut intreba" sunt concluzii DIFERITE: prima
        # justifica sa nu adaugam liga, a doua cere doar re-rulare. Amestecate intr-un singur verdict,
        # au costat trei ligi bune (Croatia, Slovacia, Ucraina) clasate gresit ca netestabile.
        $verdictGol = if ($eroareRetea) { 'EROARE' } else { 'necunoscut' }
        $mesaj = if ($eroareRetea) { 'request esuat - RE-RULEAZA, nu e o concluzie' }
                 else { 'fara meciuri terminate - nu putem testa' }
        Write-Host "       $mesaj" -ForegroundColor DarkYellow
        $rezultate += [pscustomobject]@{ Id = $c.id; Liga = $c.nume; Tara = $c.tara
            Sezon = $null; Statistici = $verdictGol; Sondate = 0; CuStats = 0
            Cornere = $null; Faulturi = $null; Cartonase = $null; CartonaseDinEvenimente = $null }
        continue
    }

    # Esantion intins uniform peste sezon, nu ultimele N: furnizorul publica statisticile cu
    # intarziere, deci coada sezonului e sistematic mai saraca decat mijlocul lui.
    $n = [math]::Min($Meciuri, $terminate.Count)
    $pas = [math]::Max(1, [int]($terminate.Count / $n))
    $esantion = @(0..($n - 1) | ForEach-Object { $terminate[[math]::Min($_ * $pas, $terminate.Count - 1)] })

    # 2. cate dintre ele au statistici, si de ce tip
    $cuStats = 0
    $cornere = 0; $faulturi = 0; $cartonase = 0
    foreach ($m in $esantion) {
        $s = Cere "fixtures/statistics?fixture=$($m.fixture.id)"
        $consum++
        $tipuri = @()
        if ($s -and $s.results -gt 0) { $tipuri = @($s.response[0].statistics | ForEach-Object { $_.type }) }
        if ($tipuri.Count -eq 0) { continue }
        $cuStats++
        if ($tipuri -contains 'Corner Kicks') { $cornere++ }
        if ($tipuri -contains 'Fouls') { $faulturi++ }
        if (($tipuri -contains 'Yellow Cards') -or ($tipuri -contains 'Red Cards')) { $cartonase++ }
    }

    # 3. plasa de siguranta: chiar fara `fixtures/statistics`, cronologia da cartonasele si golurile,
    # deci pietele de cartonase raman posibile. Cornerele si faulturile NU se pot deduce de acolo.
    $cartonaseDinEvenimente = $null
    if ($cuStats -eq 0) {
        $e = Cere "fixtures/events?fixture=$($esantion[0].fixture.id)"
        $consum++
        $cartonaseDinEvenimente = ($e -and $e.results -gt 0 -and
            @($e.response | Where-Object { $_.type -eq 'Card' }).Count -gt 0)
    }

    $verdict = if ($cuStats -eq 0) { 'DOAR SCORURI' }
               elseif ($cornere -eq $cuStats -and $faulturi -eq $cuStats -and $cartonase -eq $cuStats) { 'COMPLET' }
               else { 'PARTIAL' }

    $culoare = switch ($verdict) { 'COMPLET' { 'Green' } 'PARTIAL' { 'Yellow' } default { 'Red' } }
    Write-Host ("       sezon {0} | {1} | {2}/{3} meciuri cu stats | cornere:{4} faulturi:{5} cartonase:{6}{7}" -f `
        $sezonGasit, $verdict, $cuStats, $esantion.Count, $cornere, $faulturi, $cartonase,
        $(if ($null -ne $cartonaseDinEvenimente) { " | cartonase din cronologie: $cartonaseDinEvenimente" })) -ForegroundColor $culoare

    $rezultate += [pscustomobject]@{ Id = $c.id; Liga = $c.nume; Tara = $c.tara
        Sezon = $sezonGasit; Statistici = $verdict; Sondate = $esantion.Count; CuStats = $cuStats
        Cornere = $cornere; Faulturi = $faulturi; Cartonase = $cartonase
        CartonaseDinEvenimente = $cartonaseDinEvenimente }
}

Write-Host "`n=================== REZUMAT ==================="
$rezultate | Sort-Object Statistici, Tara | Format-Table -AutoSize
Write-Host "Requesturi consumate: $consum"
Write-Host ''
Write-Host 'DE ADAUGAT (statistici complete):'
($rezultate | Where-Object { $_.Statistici -eq 'COMPLET' } | ForEach-Object { "  $($_.Id) $($_.Liga) ($($_.Tara))" })
Write-Host 'DE SARIT (doar scoruri):'
($rezultate | Where-Object { $_.Statistici -eq 'DOAR SCORURI' } | ForEach-Object { "  $($_.Id) $($_.Liga) ($($_.Tara))" })
