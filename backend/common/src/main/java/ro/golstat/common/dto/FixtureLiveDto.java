package ro.golstat.common.dto;

import java.util.List;

/**
 * Un meci in desfasurare cu evenimentele lui inline (aduse gratis din {@code fixtures?live=all}).
 * {@code evenimente} poate fi gol la meciurile fara evenimente inca.
 */
public record FixtureLiveDto(FixtureDto fixture, List<FixtureEventDto> evenimente) {
}
