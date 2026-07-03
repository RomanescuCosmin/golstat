package ro.golstat.stats.goals;

import org.junit.jupiter.api.Test;
import ro.golstat.stats.model.MatchSample;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GoalFormTest {

  /** Doar goalsFor/goalsAgainst conteaza pentru GoalForm; restul campurilor sunt umplute neutru. */
  private static MatchSample match(int goalsFor, int goalsAgainst) {
      return new MatchSample(LocalDate.of(2024,1 ,1),
              true, goalsFor, goalsAgainst,
              0 , 0, null);
  }

  @Test
  void emptyWindow_hasNoData() {
      GoalStats stats = GoalForm.of(List.of());

      assertEquals(0, stats.sampleSize());
      assertFalse(stats.hasData());
      assertEquals(0.0, stats.scoredRate());
      assertEquals(0.0, stats.concededRate());
      assertEquals(0.0, stats.avgGoalsFor());
      assertEquals(0.0, stats.avgGoalsAgainst());
  }

  @Test
  void singleWin_rateVsAverage() {
      GoalStats stats = GoalForm.of(List.of(match(3, 0)));

      assertEquals(1, stats.sampleSize());
      assertEquals(1.0, stats.scoredRate());     // a marcat in 100% din meciuri
      assertEquals(3.0, stats.avgGoalsFor());    // dar media e 3 goluri/meci
      assertEquals(0.0, stats.concededRate());
      assertEquals(0.0, stats.avgGoalsAgainst());
  }

  @Test
  void goallessDraw_isNotEmptyWindow() {
      GoalStats stats = GoalForm.of(List.of(match(0, 0)));

      assertEquals(1, stats.sampleSize());
      assertTrue(stats.hasData());
      assertEquals(0.0, stats.scoredRate());
      assertEquals(0.0, stats.concededRate());
      assertEquals(0.0, stats.avgGoalsFor());
      assertEquals(0.0, stats.avgGoalsAgainst());
  }

  @Test
  void mixedWindow_knownNumbers() {
      GoalStats stats = GoalForm.of(List.of(match(1, 0), match(0, 0), match(2, 2)));

      assertEquals(3, stats.sampleSize());
      assertEquals(2.0 / 3, stats.scoredRate(), 1e-9);      // 1-0 si 2-2 au gol marcat
      assertEquals(1.0 / 3, stats.concededRate(), 1e-9);    // doar 2-2 a primit gol
      assertEquals(1.0, stats.avgGoalsFor(), 1e-9);         // (1+0+2)/3
      assertEquals(2.0 / 3, stats.avgGoalsAgainst(), 1e-9); // (0+0+2)/3
  }

  @Test
  void allMatchesScored_rateIsOne() {
      GoalStats stats = GoalForm.of(List.of(match(1, 0), match(2, 1), match(3, 0)));

      assertEquals(1.0, stats.scoredRate());
  }

  @Test
  void averageUsesFloatingPointDivision() {
      GoalStats stats = GoalForm.of(List.of(match(1, 0), match(0, 0)));

      assertEquals(0.5, stats.avgGoalsFor(), 1e-9); // nu 0 (ar fi impartire intreaga)
  }
}
