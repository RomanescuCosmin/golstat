package ro.golstat.api.auth;

import org.springframework.lang.Nullable;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;

@Component
class JdbcSesiuniUtilizator implements SesiuniUtilizator {

    private final FindByIndexNameSessionRepository<? extends Session> sesiuni;

    JdbcSesiuniUtilizator(FindByIndexNameSessionRepository<? extends Session> sesiuni) {
        this.sesiuni = sesiuni;
    }

    @Override
    public void invalideazaToate(long utilizatorId, @Nullable String pastreaza) {
        sesiuni.findByPrincipalName(String.valueOf(utilizatorId)).keySet().stream()
                .filter(id -> !id.equals(pastreaza))
                .forEach(sesiuni::deleteById);
    }
}
