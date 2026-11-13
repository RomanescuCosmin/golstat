package ro.golstat.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pozitia pe teren ("rand:coloana", randul 1 = portarul) a titularilor, din formatie + ordinea lor.
 * bzzoiro nu trimite grid-ul (API-Football da), doar postul (G/D/M/F) si formatia ("4-2-3-1"), cu
 * jucatorii in ordinea de pe teren: portar, apoi fiecare linie a formatiei.
 */
public final class FormatieGrid {

    private FormatieGrid() {
    }

    /**
     * @param posturi posturile titularilor in ordine (G/D/M/F)
     * @return cate un "rand:coloana" pentru fiecare titular, in aceeasi ordine
     */
    public static List<String> grid(List<String> posturi, String formatie) {
        int[] linii = linii(formatie);
        if (linii.length > 0 && Arrays.stream(linii).sum() + 1 == posturi.size()) {
            List<String> out = new ArrayList<>(List.of("1:1"));
            for (int r = 0; r < linii.length; r++) {
                for (int c = 1; c <= linii[r]; c++) {
                    out.add((r + 2) + ":" + c);
                }
            }
            return out;
        }
        // formatie necunoscuta sau numar diferit de jucatori: cate un rand pe post
        int[] coloane = new int[5];
        List<String> out = new ArrayList<>();
        for (String post : posturi) {
            int rand = rand(post);
            out.add(rand + ":" + (++coloane[rand]));
        }
        return out;
    }

    /** Ordinea pe teren a posturilor: G, D, M, F (necunoscut → mijloc). */
    public static int rand(String post) {
        if (post == null || post.isBlank()) {
            return 3;
        }
        return switch (Character.toUpperCase(post.charAt(0))) {
            case 'G' -> 1;
            case 'D' -> 2;
            case 'F', 'A' -> 4;
            default -> 3;
        };
    }

    private static int[] linii(String formatie) {
        if (formatie == null || !formatie.matches("\\d+(-\\d+)+")) {
            return new int[0];
        }
        return Arrays.stream(formatie.split("-")).mapToInt(Integer::parseInt).toArray();
    }
}
