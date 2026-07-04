package ro.golstat.collector.provider.apifootball;

import java.util.List;

/**
 * Un element din {@code /coachs?team=}: profilul antrenorului + {@code career[]}. Antrenorul CURENT
 * al echipei cerute = intrarea de cariera cu {@code end == null} pe acea echipa.
 */
public record CoachItem(Long id, String name, String firstname, String lastname, Integer age,
                        String nationality, String photo, Team team, List<Career> career) {

    public record Team(Long id) {
    }

    public record Career(Team team, String start, String end) {
    }
}
