package pl.codeleak.slack.sleuth;

import lombok.Builder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Builder
final class TimeRange {

    LocalDateTime from;
    LocalDateTime to;

    String from() {
        if (Objects.isNull(from)) {
            return "0";
        }
        return from.toInstant(ZoneOffset.UTC).getEpochSecond() + "";
    }

    String to() {
        if (Objects.isNull(to)) {
            return Instant.now().getEpochSecond() + "";
        }
        return to.toInstant(ZoneOffset.UTC).getEpochSecond() + "";
    }
}
