package ro.golstat.collector.provider.apifootball;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Envelope-ul comun tuturor endpoint-urilor API-Football: {@code response} e lista utila,
 * {@code errors} e {@code []} cand nu-s erori sau un obiect {@code {cheie: mesaj}} cand exista
 * (de-aceea {@link JsonNode}, nu tip fix). {@code results} = numarul de elemente. {@code paging}
 * apare la endpoint-urile paginate (ex. {@code /players}); absent → o singura pagina.
 */
public record ApiFootballResponse<T>(List<T> response, Paging paging, JsonNode errors, Integer results) {

    /** Paginarea API-Football: {@code current} = pagina curenta, {@code total} = numarul de pagini. */
    public record Paging(Integer current, Integer total) {
    }
}
