package ro.golstat.api.piete;

import ro.golstat.api.entity.Fixture;
import ro.golstat.api.entity.FixtureTeamStats;
import ro.golstat.api.entity.League;
import ro.golstat.api.entity.Standing;
import ro.golstat.api.entity.Team;
import ro.golstat.api.repository.FixtureRepository;
import ro.golstat.api.repository.FixtureTeamStatsRepository;
import ro.golstat.api.repository.LeagueRepository;
import ro.golstat.api.repository.StandingRepository;
import ro.golstat.api.repository.TeamRepository;
import ro.golstat.api.stats.CountAverage;
import ro.golstat.api.stats.GoalAverage;
import ro.golstat.api.stats.RefereeCardAverage;
import ro.golstat.api.stats.RefereeCardAverageRow;
import ro.golstat.common.GolstatConstants;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Un set de date in memorie servit prin repository-uri simulate — ACELEASI date alimenteaza si
 * calea per-meci ({@code MatchPreviewService}) si cea in bloc ({@code PieteZileService}), ca testul
 * de echivalenta sa compare doua calcule reale, nu doua seturi de mock-uri potrivite intre ele.
 *
 * <p>Fiecare repository numara apelurile ({@link #apeluri}), pentru testul care pazeste bugetul de
 * query-uri impotriva reintroducerii unui N+1.
 */
final class DateDeTest {

    private static final List<String> TERMINAL = GolstatConstants.FixtureStatus.TERMINAL.stream().toList();

    final List<Fixture> meciuri = new ArrayList<>();
    final List<FixtureTeamStats> statistici = new ArrayList<>();
    final List<Standing> clasament = new ArrayList<>();
    final List<Team> echipe = new ArrayList<>();
    final List<League> ligi = new ArrayList<>();

    final AtomicInteger apeluri = new AtomicInteger();

    final FixtureRepository fixtures = mock(FixtureRepository.class);
    final FixtureTeamStatsRepository teamStats = mock(FixtureTeamStatsRepository.class);
    final StandingRepository standings = mock(StandingRepository.class);
    final TeamRepository teams = mock(TeamRepository.class);
    final LeagueRepository leagues = mock(LeagueRepository.class);

    /** Se apeleaza dupa ce datele au fost populate. */
    @SuppressWarnings("unchecked")
    DateDeTest conecteaza() {
        when(fixtures.findById(anyLong())).thenAnswer(i -> {
            apeluri.incrementAndGet();
            long id = i.getArgument(0);
            return meciuri.stream().filter(f -> f.getId() == id).findFirst();
        });

        when(fixtures.findRecentForTeam(anyLong(), any(), any(), any())).thenAnswer(i -> {
            apeluri.incrementAndGet();
            long teamId = i.getArgument(0);
            Collection<String> terminal = i.getArgument(1);
            OffsetDateTime before = i.getArgument(2);
            org.springframework.data.domain.Pageable pageable = i.getArgument(3);
            return istoric(teamId, terminal, before).stream()
                    .limit(pageable.getPageSize())
                    .toList();
        });

        when(fixtures.findTerminalForTeams(any(), any(), any())).thenAnswer(i -> {
            apeluri.incrementAndGet();
            Collection<Long> ids = i.getArgument(0);
            Collection<String> terminal = i.getArgument(1);
            OffsetDateTime before = i.getArgument(2);
            return meciuri.stream()
                    .filter(f -> f.getStatusShort() != null && terminal.contains(f.getStatusShort()))
                    .filter(f -> f.getKickoff() != null && f.getKickoff().isBefore(before))
                    .filter(f -> ids.contains(f.getHomeTeamId()) || ids.contains(f.getAwayTeamId()))
                    .sorted(Comparator.comparing(Fixture::getKickoff).reversed())
                    .toList();
        });

        when(fixtures.findUpcomingAll(any(), any(), any())).thenAnswer(i -> {
            apeluri.incrementAndGet();
            String status = i.getArgument(0);
            OffsetDateTime from = i.getArgument(1);
            OffsetDateTime to = i.getArgument(2);
            return meciuri.stream()
                    .filter(f -> status.equals(f.getStatusShort()))
                    .filter(f -> f.getKickoff() != null
                            && !f.getKickoff().isBefore(from) && f.getKickoff().isBefore(to))
                    .sorted(Comparator.comparing(Fixture::getKickoff))
                    .toList();
        });

        when(fixtures.findHeadToHead(anyLong(), anyLong(), any(), any(), any())).thenAnswer(i -> {
            apeluri.incrementAndGet();
            return List.of();
        });

        when(fixtures.avgGoals(anyLong(), org.mockito.ArgumentMatchers.anyInt(), any())).thenAnswer(i -> {
            apeluri.incrementAndGet();
            long leagueId = i.getArgument(0);
            int season = i.getArgument(1);
            List<Fixture> terminale = meciuri.stream()
                    .filter(f -> f.getStatusShort() != null && TERMINAL.contains(f.getStatusShort()))
                    .filter(f -> leagueId == nz(f.getLeagueId()) && season == nz(f.getSeasonYear()))
                    .toList();
            if (terminale.isEmpty()) {
                return (GoalAverage) proiectieGoluri(null, null);
            }
            double gazde = terminale.stream().mapToInt(f -> scor(f.getScoreFtHome(), f.getGoalsHome())).average().orElse(0);
            double oaspeti = terminale.stream().mapToInt(f -> scor(f.getScoreFtAway(), f.getGoalsAway())).average().orElse(0);
            return proiectieGoluri(gazde, oaspeti);
        });

        when(teamStats.findByFixtureIdIn(any())).thenAnswer(i -> {
            apeluri.incrementAndGet();
            Collection<Long> ids = i.getArgument(0);
            return statistici.stream().filter(s -> ids.contains(s.getFixtureId())).toList();
        });

        when(teamStats.avgCounts(anyLong(), org.mockito.ArgumentMatchers.anyInt(), any())).thenAnswer(i -> {
            apeluri.incrementAndGet();
            long leagueId = i.getArgument(0);
            int season = i.getArgument(1);
            Map<Long, Fixture> perId = meciuri.stream()
                    .collect(Collectors.toMap(Fixture::getId, f -> f, (a, b) -> a));
            List<FixtureTeamStats> randuri = statistici.stream()
                    .filter(s -> {
                        Fixture f = perId.get(s.getFixtureId());
                        return f != null && f.getStatusShort() != null && TERMINAL.contains(f.getStatusShort())
                                && leagueId == nz(f.getLeagueId()) && season == nz(f.getSeasonYear());
                    })
                    .toList();
            if (randuri.isEmpty()) {
                return proiectieCounturi(null, null, null, null, null);
            }
            return proiectieCounturi(
                    medie(randuri, FixtureTeamStats::getCornerKicks),
                    medie(randuri, FixtureTeamStats::getFouls),
                    medie(randuri, s -> suma(s.getYellowCards(), s.getRedCards())),
                    medie(randuri, FixtureTeamStats::getShotsTotal),
                    medie(randuri, FixtureTeamStats::getShotsOnGoal));
        });

        when(teamStats.refereeCardAverage(any(), any())).thenAnswer(i -> {
            apeluri.incrementAndGet();
            String referee = i.getArgument(0);
            return arbitru(referee);
        });

        when(teamStats.refereeCardAverages(any(), any())).thenAnswer(i -> {
            apeluri.incrementAndGet();
            Collection<String> nume = i.getArgument(0);
            List<RefereeCardAverageRow> out = new ArrayList<>();
            for (String n : nume) {
                RefereeCardAverage agg = arbitru(n);
                if (agg.getMatches() != null && agg.getMatches() != 0) {
                    out.add(proiectieArbitruRand(n, agg.getAvgCards(), agg.getMatches()));
                }
            }
            return out;
        });

        when(standings.findById(any())).thenAnswer(i -> {
            apeluri.incrementAndGet();
            Standing.Pk pk = i.getArgument(0);
            return clasament.stream()
                    .filter(s -> pk.equals(new Standing.Pk(s.getLeagueId(), s.getSeasonYear(), s.getTeamId())))
                    .findFirst();
        });
        when(standings.findAll()).thenAnswer(i -> {
            apeluri.incrementAndGet();
            return List.copyOf(clasament);
        });

        when(teams.findAllById(any())).thenAnswer(i -> {
            apeluri.incrementAndGet();
            Collection<Long> ids = (Collection<Long>) i.getArgument(0);
            return echipe.stream().filter(t -> ids.contains(t.getId())).toList();
        });
        when(leagues.findAllById(any())).thenAnswer(i -> {
            apeluri.incrementAndGet();
            Collection<Long> ids = (Collection<Long>) i.getArgument(0);
            return ligi.stream().filter(l -> ids.contains(l.getId())).toList();
        });

        return this;
    }

    private List<Fixture> istoric(long teamId, Collection<String> terminal, OffsetDateTime before) {
        return meciuri.stream()
                .filter(f -> f.getStatusShort() != null && terminal.contains(f.getStatusShort()))
                .filter(f -> f.getKickoff() != null && f.getKickoff().isBefore(before))
                .filter(f -> eq(f.getHomeTeamId(), teamId) || eq(f.getAwayTeamId(), teamId))
                .sorted(Comparator.comparing(Fixture::getKickoff).reversed())
                .toList();
    }

    private RefereeCardAverage arbitru(String referee) {
        Map<Long, Fixture> perId = meciuri.stream()
                .collect(Collectors.toMap(Fixture::getId, f -> f, (a, b) -> a));
        List<FixtureTeamStats> randuri = statistici.stream()
                .filter(s -> {
                    Fixture f = perId.get(s.getFixtureId());
                    return f != null && referee.equals(f.getReferee())
                            && f.getStatusShort() != null && TERMINAL.contains(f.getStatusShort());
                })
                .toList();
        if (randuri.isEmpty()) {
            return proiectieArbitru(null, 0L);
        }
        double medie = randuri.stream().mapToInt(s -> suma(s.getYellowCards(), s.getRedCards())).average().orElse(0);
        long meciuriArbitrate = randuri.stream().map(FixtureTeamStats::getFixtureId).distinct().count();
        return proiectieArbitru(2 * medie, meciuriArbitrate);
    }

    // --- proiectii (interfetele de proiectie Spring, implementate ca lambda-uri anonime) ---

    private static GoalAverage proiectieGoluri(Double gazde, Double oaspeti) {
        return new GoalAverage() {
            public Double getAvgGazde() {
                return gazde;
            }

            public Double getAvgOaspeti() {
                return oaspeti;
            }
        };
    }

    private static CountAverage proiectieCounturi(Double cornere, Double faulturi, Double cartonase,
                                                  Double suturi, Double suturiPePoarta) {
        return new CountAverage() {
            public Double getAvgCornere() {
                return cornere;
            }

            public Double getAvgFaulturi() {
                return faulturi;
            }

            public Double getAvgCartonase() {
                return cartonase;
            }

            public Double getAvgSuturi() {
                return suturi;
            }

            public Double getAvgSuturiPePoarta() {
                return suturiPePoarta;
            }
        };
    }

    private static RefereeCardAverage proiectieArbitru(Double avg, Long matches) {
        return new RefereeCardAverage() {
            public Double getAvgCards() {
                return avg;
            }

            public Long getMatches() {
                return matches;
            }
        };
    }

    private static RefereeCardAverageRow proiectieArbitruRand(String referee, Double avg, Long matches) {
        return new RefereeCardAverageRow() {
            public String getReferee() {
                return referee;
            }

            public Double getAvgCards() {
                return avg;
            }

            public Long getMatches() {
                return matches;
            }
        };
    }

    // --- ajutoare ---

    private static double medie(List<FixtureTeamStats> randuri,
                                java.util.function.ToIntFunction<FixtureTeamStats> camp) {
        return randuri.stream().mapToInt(camp).average().orElse(0);
    }

    private static int suma(Integer a, Integer b) {
        return (a != null ? a : 0) + (b != null ? b : 0);
    }

    private static int scor(Integer scoreFt, Integer goals) {
        if (scoreFt != null) {
            return scoreFt;
        }
        return goals != null ? goals : 0;
    }

    private static boolean eq(Long a, long b) {
        return a != null && a == b;
    }

    private static long nz(Long v) {
        return v != null ? v : -1;
    }

    private static int nz(Integer v) {
        return v != null ? v : -1;
    }

    Optional<Fixture> meci(long id) {
        return meciuri.stream().filter(f -> f.getId() == id).findFirst();
    }
}
