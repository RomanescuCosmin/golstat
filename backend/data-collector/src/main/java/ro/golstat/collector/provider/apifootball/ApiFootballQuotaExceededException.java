package ro.golstat.collector.provider.apifootball;

/** Cota zilnica API-Football a fost atinsa; cererea a fost blocata inainte de a lovi API-ul. */
public class ApiFootballQuotaExceededException extends ApiFootballException {

    public ApiFootballQuotaExceededException(String path) {
        super("Cota zilnica API-Football atinsa; sar peste " + path);
    }
}
