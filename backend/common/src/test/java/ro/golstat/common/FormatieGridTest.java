package ro.golstat.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FormatieGridTest {

    @Test
    void formatie4231_impartePeLinii() {
        List<String> posturi = List.of("G", "D", "D", "D", "D", "M", "M", "M", "M", "M", "F");
        assertEquals(List.of("1:1", "2:1", "2:2", "2:3", "2:4", "3:1", "3:2", "4:1", "4:2", "4:3", "5:1"),
                FormatieGrid.grid(posturi, "4-2-3-1"));
    }

    @Test
    void faraFormatie_cateUnRandPePost() {
        List<String> posturi = List.of("G", "D", "D", "M", "F", "F");
        assertEquals(List.of("1:1", "2:1", "2:2", "3:1", "4:1", "4:2"), FormatieGrid.grid(posturi, null));
    }
}
