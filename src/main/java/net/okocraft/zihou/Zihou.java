package net.okocraft.zihou;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.VisibleForTesting;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
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
        ZonedDateTime now = ZonedDateTime.now(this.clock);
        ZonedDateTime next = now.truncatedTo(ChronoUnit.HOURS).plusHours(1);
        return Duration.between(now.toInstant(), next.toInstant());
    }

    @VisibleForTesting
    LocalDateTime getAdjustedNow() {
        LocalDateTime now = LocalDateTime.now(this.clock);
        return now.getSecond() == 59 ? now.truncatedTo(ChronoUnit.HOURS).plusHours(1) : now;
    }

}
