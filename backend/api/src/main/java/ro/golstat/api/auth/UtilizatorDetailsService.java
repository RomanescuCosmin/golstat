package ro.golstat.api.auth;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.golstat.api.repository.UtilizatorRepository;

@Service
public class UtilizatorDetailsService implements UserDetailsService, UserDetailsPasswordService {

    private final UtilizatorRepository utilizatori;

    public UtilizatorDetailsService(UtilizatorRepository utilizatori) {
        this.utilizatori = utilizatori;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        return utilizatori.findByEmail(NormalizatorEmail.normalizeaza(email))
                .map(DetaliiUtilizator::din)
                .orElseThrow(() -> new UsernameNotFoundException("necunoscut"));
    }

    /** Re-hash automat cand parametrii Argon2 se schimba ({@code upgradeEncoding}). */
    @Override
    @Transactional
    public UserDetails updatePassword(UserDetails user, String parolaHashNoua) {
        var u = utilizatori.findById(((DetaliiUtilizator) user).id()).orElseThrow();
        u.setParolaHash(parolaHashNoua);
        return DetaliiUtilizator.din(u);
    }
}
