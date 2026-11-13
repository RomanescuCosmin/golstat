package ro.golstat.api.stats;

import org.springframework.stereotype.Service;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.common.GolstatConstants;
import ro.golstat.stats.goals.ForteAdversari;
import ro.golstat.stats.model.MatchSample;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Forta adversarilor intalniti in ferestrele de forma: golurile lor marcate / primite pe meci din toate
 * meciurile terminate dinaintea unei date (orice competitie), in 3 query-uri indiferent de numarul de echipe.
 */
@Service
public class ForteAdversariService {

    private static final List<String> TERMINAL = List.of(
            GolstatConstants.FixtureStatus.FINISHED,
            GolstatConstants.FixtureStatus.FINISHED_AET,
            GolstatConstants.FixtureStatus.FINISHED_PEN
    );

    private final FixtureRepository fixtures;

    public ForteAdversariService(FixtureRepository fixtures) {
        this.fixtures = fixtures;
    }

    /** Forta adversarilor care apar in istoricele date. */
    public ForteAdversari pentru(Collection<List<MatchSample>> istorice, OffsetDateTime before) {
        Set<Long> adversari = istorice.stream()
                .flatMap(List::stream)
                .map(MatchSample::opponentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return forte(adversari, before);
    }

    public ForteAdversari forte(Collection<Long> echipe, OffsetDateTime before) {
        if (echipe.isEmpty() || before == null) {
            return ForteAdversari.fara();
        }
        Map<Long, long[]> sume = new HashMap<>();
        Stream.concat(fixtures.goluriCaGazde(echipe, TERMINAL, before).stream(),
                        fixtures.goluriCaOaspeti(echipe, TERMINAL, before).stream())
                .filter(r -> r.getTeamId() != null && r.getMeciuri() != null)
                .forEach(r -> {
                    long[] s = sume.computeIfAbsent(r.getTeamId(), k -> new long[3]);
                    s[0] += r.getMarcate() != null ? r.getMarcate() : 0;
                    s[1] += r.getPrimite() != null ? r.getPrimite() : 0;
                    s[2] += r.getMeciuri();
                });
        Map<Long, ForteAdversari.Forta> forte = new HashMap<>();
        sume.forEach((echipa, s) -> {
            if (s[2] > 0) {
                forte.put(echipa, new ForteAdversari.Forta((double) s[0] / s[2], (double) s[1] / s[2], (int) s[2]));
            }
        });
        Double medie = fixtures.goluriPeEchipaPeMeci(TERMINAL, before);
        return new ForteAdversari(forte, medie != null ? medie : 0);
    }
}
