package ro.golstat.api.team;

import ro.golstat.api.prediction.PredictieMeciDto.EchipaDto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Pagina unei echipe: antet, sumar sezon, forma recenta, urmatorul meci, snippet clasament,
 * bare de statistici, distributia golurilor pe intervale si top jucatori. Fiecare bloc degradeaza
 * independent la {@code null}/{@code []} — pagina raspunde mereu 200 daca echipa exista.
 */
public record PaginaEchipaDto(
        Antet antet,
        Sumar sumar,
        List<MeciForma> forma,
        List<MeciForma> rezultateRecente,
        List<StatProcent> statProcente,
        List<Integer> sezoane,
        MeciScurt urmatorulMeci,
        List<RandClasament> clasament,
        StatBare statistici,
        List<Bucket> goluriPeInterval,
        TopJucatori topJucatori
) {
    /** Identitatea echipei + contextul de sezon; {@code antrenor}/{@code stadion} pot lipsi. */
    public record Antet(
            long teamId,
            String nume,
            String logo,
            String tara,
            String liga,
            String ligaLogo,
            Long leagueId,
            Integer sezon,
            String antrenor,
            String stadion,
            Integer capacitate
    ) {
    }

    /** Sumarul sezonului: pozitie/puncte din clasament, restul din statisticile de sezon. */
    public record Sumar(
            Integer pozitie,
            Integer puncte,
            Integer jucate,
            Integer victorii,
            Integer egaluri,
            Integer infrangeri,
            Integer goluriMarcate,
            Integer goluriPrimite,
            Integer golaveraj
    ) {
    }

    /** Un meci din forma recenta (ultimele 5). {@code rezultat} = "V"/"E"/"I". */
    public record MeciForma(
            long fixtureId,
            OffsetDateTime data,
            boolean acasa,
            EchipaDto adversar,
            Integer golMarcate,
            Integer golPrimite,
            String rezultat
    ) {
    }

    /**
     * Procentul unei categorii relativ la media ligii: {@code procent = 100 × medieEchipa / (2 × medieLiga)},
     * plafonat [0,100] (50% = exact media ligii). Afisam si valoarea echipei si media ligii.
     */
    public record StatProcent(String categorie, Double medieEchipa, Double medieLiga, Integer procent) {
    }

    /** Urmatorul meci programat al echipei. */
    public record MeciScurt(
            long fixtureId,
            OffsetDateTime kickoff,
            EchipaDto adversar,
            boolean acasa
    ) {
    }

    /** Un rand din clasament; {@code echipaCurenta} = randul echipei acestei pagini. */
    public record RandClasament(
            Integer rank,
            long teamId,
            String nume,
            String logo,
            Integer jucate,
            Integer puncte,
            Integer golaveraj,
            boolean echipaCurenta
    ) {
    }

    /** Bare de statistici pe sezon; mediile de suturi/posesie/pase pot lipsi (fara statistici colectate). */
    public record StatBare(
            Double goluriMarcatePeMeci,
            Double goluriPrimitePeMeci,
            Integer cleanSheets,
            Integer galbene,
            Integer rosii,
            Double suturiPeMeci,
            Double posesieMedie,
            Double preciziePase
    ) {
    }

    /** Numarul de goluri marcate intr-un interval de timp (ex. "1-15", "90+"). */
    public record Bucket(String interval, int goluri) {
    }

    /** Cei mai buni jucatori pe cateva categorii; oricare poate lipsi. */
    public record TopJucatori(Jucator golgheter, Jucator pasator, Jucator minute, Jucator cartonase) {
    }

    /** Un jucator din top; {@code valoare} = metrica categoriei (goluri, pase, minute, cartonase). */
    public record Jucator(Long playerId, String nume, String foto, int valoare) {
    }
}
