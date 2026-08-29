package net.okocraft.zihou;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.stream.Stream;

class ZihouTest {

    @ParameterizedTest
    @MethodSource("getAdjustedNowTestCases")
    void getAdjustedNow(GetAdjustedNowTestCase testCase) {
        Clock clock = Clock.fixed(testCase.now.atZone(testCase.zone).toInstant(), testCase.zone);
        Zihou zihou = new Zihou(clock, _ -> Component.empty());
        Assertions.assertEquals(testCase.expected, zihou.getAdjustedNow());
    }

    private record GetAdjustedNowTestCase(ZoneId zone, LocalDateTime now, LocalDateTime expected) {
        @Override
        public String toString() {
            return this.zone + " " + this.now.toString() + " -> " + this.expected;
        }
    }

    private static Stream<GetAdjustedNowTestCase> getAdjustedNowTestCases() {
        return Stream.of(
                ZoneOffset.UTC,
                ZoneId.of("Australia/Adelaide"),
                ZoneId.of("Asia/Kathmandu"),
                ZoneId.of("Asia/Tokyo")
            )
            .flatMap(zone -> Stream.of(
                new GetAdjustedNowTestCase(zone, LocalDateTime.of(2025, 1, 2, 2, 59, 59), LocalDateTime.of(2025, 1, 2, 3, 0, 0)),
                new GetAdjustedNowTestCase(zone, LocalDateTime.of(2025, 1, 2, 3, 0, 0), LocalDateTime.of(2025, 1, 2, 3, 0, 0))
            ));
    }

    @ParameterizedTest
    @MethodSource("calculateTaskDelayTestCases")
    void calculateTaskDelay(CalculateTaskDelayTestCase testCase) {
        Zihou zihou = new Zihou(Clock.fixed(testCase.now, testCase.zone), _ -> Component.empty());
        Assertions.assertEquals(testCase.expectedDelay, zihou.calculateTaskDelay());
    }

    private record CalculateTaskDelayTestCase(ZoneId zone, Instant now, Duration expectedDelay) {
        @Override
        public String toString() {
            return this.zone + " " + this.now.atZone(this.zone) + " -> " + this.expectedDelay;
        }
    }

    private static Stream<CalculateTaskDelayTestCase> calculateTaskDelayTestCases() {
        return Stream.concat(fixedOffsetTestCases(), dstTestCases());
    }

    private static Stream<CalculateTaskDelayTestCase> fixedOffsetTestCases() {
        return Stream.of(
                ZoneOffset.UTC,
                ZoneId.of("Australia/Adelaide"),
                ZoneId.of("Asia/Kathmandu"),
                ZoneId.of("Asia/Tokyo")
            )
            .flatMap(zone -> Stream.of(
                new CalculateTaskDelayTestCase(zone, LocalDateTime.of(2025, 1, 2, 2, 58, 30).atZone(zone).toInstant(), Duration.ofSeconds(90)),
                new CalculateTaskDelayTestCase(zone, LocalDateTime.of(2025, 1, 2, 2, 59, 59).atZone(zone).toInstant(), Duration.ofSeconds(1)),
                new CalculateTaskDelayTestCase(zone, LocalDateTime.of(2025, 1, 2, 3, 0, 0).atZone(zone).toInstant(), Duration.ofHours(1)),
                new CalculateTaskDelayTestCase(zone, LocalDateTime.of(2025, 1, 2, 3, 0, 1).atZone(zone).toInstant(), Duration.ofHours(1).minusSeconds(1)),
                new CalculateTaskDelayTestCase(zone, LocalDateTime.of(2025, 1, 2, 3, 1, 30).atZone(zone).toInstant(), Duration.ofHours(1).minusSeconds(90))
            ));
    }

    private static Stream<CalculateTaskDelayTestCase> dstTestCases() {
        ZoneId lordHowe = ZoneId.of("Australia/Lord_Howe");
        ZoneId newYork = ZoneId.of("America/New_York");
        return Stream.of(
            // Australia/Lord_Howe: 2026-10-04 02:00 +10:30 -> 02:30 +11:00 (local 02:00 does not exist)
            new CalculateTaskDelayTestCase(lordHowe, Instant.parse("2026-10-03T14:30:00Z"), Duration.ofMinutes(90)), // local 01:00 +10:30 -> 03:00 +11:00
            new CalculateTaskDelayTestCase(lordHowe, Instant.parse("2026-10-03T15:29:00Z"), Duration.ofMinutes(31)), // local 01:59 +10:30 -> 03:00 +11:00
            new CalculateTaskDelayTestCase(lordHowe, Instant.parse("2026-10-03T15:30:00Z"), Duration.ofMinutes(30)), // local 02:30 +11:00 -> 03:00 +11:00
            // Australia/Lord_Howe: 2026-04-05 02:00 +11:00 -> 01:30 +10:30 (local 01:30-01:59 occurs twice)
            new CalculateTaskDelayTestCase(lordHowe, Instant.parse("2026-04-04T14:45:00Z"), Duration.ofMinutes(45)), // local 01:45 +11:00 -> 02:00 +10:30
            new CalculateTaskDelayTestCase(lordHowe, Instant.parse("2026-04-04T15:15:00Z"), Duration.ofMinutes(15)), // local 01:45 +10:30 -> 02:00 +10:30
            // America/New_York: 2026-11-01 02:00 -04:00 -> 01:00 -05:00 (local 01:00 occurs twice)
            new CalculateTaskDelayTestCase(newYork, Instant.parse("2026-11-01T05:30:00Z"), Duration.ofMinutes(30)), // local 01:30 -04:00 -> 01:00 -05:00
            new CalculateTaskDelayTestCase(newYork, Instant.parse("2026-11-01T06:00:00Z"), Duration.ofHours(1)) // local 01:00 -05:00 -> 02:00 -05:00
        );
    }
}
