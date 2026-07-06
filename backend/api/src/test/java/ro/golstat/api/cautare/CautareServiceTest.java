package ro.golstat.api.cautare;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import ro.golstat.api.entity.League;
import ro.golstat.api.entity.Player;
import ro.golstat.api.entity.Team;
import ro.golstat.api.repository.LeagueRepository;
import ro.golstat.api.repository.PlayerRepository;
import ro.golstat.api.repository.TeamRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CautareServiceTest {

    @Mock TeamRepository teams;
    @Mock LeagueRepository leagues;
    @Mock PlayerRepository players;
    @InjectMocks CautareService service;

    @Test
    void subDouaCaractere_returneazaGol_faraHitDB() {
        List<RezultatCautareDto> r = service.cauta("m");

        assertTrue(r.isEmpty());
        verifyNoInteractions(teams, leagues, players);
    }

    @Test
    void normalizeazaTermenul_stripSiLowercase() {
        when(teams.search(eq("man"), any())).thenReturn(List.of(team(50L, "Manchester City")));

        service.cauta("  MAN ");

        // termenul ajunge la repository deja curatat si lowercased
        org.mockito.Mockito.verify(teams).search(eq("man"), any());
    }

    @Test
    void combinaCeleTreiTipuri_inOrdineEchipeLigiJucatori() {
        Team club = team(50L, "Real Madrid");
        club.setIsNational(false);
        Team nat = team(768L, "Spain");
        nat.setIsNational(true);
        nat.setCountryName("Spain");
        when(teams.search(eq("real"), any())).thenReturn(List.of(club, nat));
        when(leagues.search(eq("real"), any())).thenReturn(List.of(league(140L, "La Liga", "Spain")));
        when(players.search(eq("real"), any())).thenReturn(List.of(player(999L, "Real Nume", "Brazil")));

        List<RezultatCautareDto> r = service.cauta("Real");

        assertEquals(4, r.size());
        // ordinea: echipe intai, apoi campionate, apoi jucatori
        assertEquals(TipRezultat.ECHIPA, r.get(0).tip());
        assertEquals(50L, r.get(0).id());
        assertEquals("Real Madrid", r.get(0).nume());
        assertEquals("Spain", r.get(0).subtitlu());
        assertFalse(r.get(0).nationala());
        assertTrue(r.get(1).nationala());   // nationala Spania

        assertEquals(TipRezultat.LIGA, r.get(2).tip());
        assertEquals(140L, r.get(2).id());
        assertEquals("La Liga", r.get(2).nume());
        assertEquals("Spain", r.get(2).subtitlu());
        assertFalse(r.get(2).nationala());

        assertEquals(TipRezultat.JUCATOR, r.get(3).tip());
        assertEquals(999L, r.get(3).id());
        assertEquals("Real Nume", r.get(3).nume());
        assertEquals("Brazil", r.get(3).subtitlu());   // subtitlu jucator = nationalitate
    }

    private static Team team(long id, String nume) {
        Team t = new Team();
        t.setId(id);
        t.setName(nume);
        t.setLogo("http://logo/" + id + ".png");
        t.setCountryName("Spain");
        return t;
    }

    private static League league(long id, String nume, String tara) {
        League l = new League();
        l.setId(id);
        l.setName(nume);
        l.setLogo("http://logo/l" + id + ".png");
        l.setCountryName(tara);
        return l;
    }

    private static Player player(long id, String nume, String nationalitate) {
        Player p = new Player();
        p.setId(id);
        p.setName(nume);
        p.setPhoto("http://photo/" + id + ".png");
        p.setNationality(nationalitate);
        return p;
    }
}
