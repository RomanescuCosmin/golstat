package ro.golstat.api.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;

import java.util.Map;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JdbcSesiuniUtilizatorTest {

    @Mock FindByIndexNameSessionRepository<MapSession> repo;

    @Test
    void invalideazaToate_stergeToateCuExceptiaCeleiPastrate() {
        when(repo.findByPrincipalName("7")).thenReturn(Map.of(
                "s1", new MapSession("s1"), "s2", new MapSession("s2"), "curenta", new MapSession("curenta")));

        new JdbcSesiuniUtilizator(repo).invalideazaToate(7, "curenta");

        verify(repo).deleteById("s1");
        verify(repo).deleteById("s2");
        verify(repo, never()).deleteById("curenta");
    }

    @Test
    void invalideazaToate_faraPastrare_stergeTot() {
        when(repo.findByPrincipalName("7")).thenReturn(Map.of("s1", new MapSession("s1")));

        new JdbcSesiuniUtilizator(repo).invalideazaToate(7, null);

        verify(repo).deleteById("s1");
    }
}
