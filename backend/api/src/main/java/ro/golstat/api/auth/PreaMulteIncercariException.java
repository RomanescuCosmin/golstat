package ro.golstat.api.auth;

public class PreaMulteIncercariException extends RuntimeException {

    private final long retryAfterSecunde;

    public PreaMulteIncercariException(long retryAfterSecunde) {
        super("Prea multe încercări. Reîncearcă mai târziu.");
        this.retryAfterSecunde = retryAfterSecunde;
    }

    public long getRetryAfterSecunde() {
        return retryAfterSecunde;
    }
}
