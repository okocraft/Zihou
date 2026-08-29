package net.okocraft.zihou;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.VisibleForTesting;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.time.zone.ZoneRules;
import java.util.function.Function;

public record Zihou(Clock clock, Function<TemporalAccessor, Component> toMessage) {

    public static Zihou create(ZihouConfig config) {
        ZoneId zoneId = config.tryParseTimezoneId();
        return new Zihou(
            zoneId == null ? Clock.systemDefaultZone() : Clock.system(zoneId),
            config::createMessageComponent
        );
    }

    public void announce(Audience audience) {
        audience.sendMessage(this.toMessage.apply(getAdjustedNow()));
    }

    public Duration calculateTaskDelay() {
        ZoneId zone = this.clock.getZone();
        ZoneRules rules = zone.getRules();
        Instant now = this.clock.instant();

        LocalDateTime candidate = LocalDateTime.ofInstant(now, zone).truncatedTo(ChronoUnit.HOURS);

        while (true) {
            for (ZoneOffset offset : rules.getValidOffsets(candidate)) { // empty if the local hour is in a DST gap
                Instant instant = candidate.toInstant(offset);
                if (instant.isAfter(now)) {
                    return Duration.between(now, instant);
                }
            }
            candidate = candidate.plusHours(1);
        }
    }

    @VisibleForTesting
    LocalDateTime getAdjustedNow() {
        LocalDateTime now = LocalDateTime.now(this.clock);
        return now.getMinute() == 59 && now.getSecond() == 59 ? now.truncatedTo(ChronoUnit.HOURS).plusHours(1) : now;
    }

}
