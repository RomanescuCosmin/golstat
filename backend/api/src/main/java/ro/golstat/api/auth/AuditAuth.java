package ro.golstat.api.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Jurnal de evenimente de autentificare. Nu scrie niciodata email, parola sau token. */
@Component
public class AuditAuth {

    private static final Logger log = LoggerFactory.getLogger("golstat.audit");

    public void eveniment(String eveniment, @Nullable Long utilizatorId) {
        log.info("eveniment={} utilizator={} ip={}", eveniment,
                utilizatorId == null ? "necunoscut" : utilizatorId, ipCurent());
    }

    private static String ipCurent() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a
                ? a.getRequest().getRemoteAddr()
                : "-";
    }
}
