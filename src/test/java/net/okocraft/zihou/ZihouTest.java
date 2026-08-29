package net.okocraft.zihou;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Duration;
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
        Clock clock = Clock.fixed(testCase.now.atZone(testCase.zone).toInstant(), testCase.zone);
        Zihou zihou = new Zihou(clock, _ -> Component.empty());
        Assertions.assertEquals(testCase.expectedDelay, zihou.calculateTaskDelay());
    }

    private record CalculateTaskDelayTestCase(ZoneId zone, LocalDateTime now, Duration expectedDelay) {
        @Override
        public String toString() {
            return this.zone + " " + this.now + " -> " + this.expectedDelay;
        }
    }

    private static Stream<CalculateTaskDelayTestCase> calculateTaskDelayTestCases() {
        return Stream.of(
                ZoneOffset.UTC,
                ZoneId.of("Australia/Adelaide"),
                ZoneId.of("Asia/Kathmandu"),
                ZoneId.of("Asia/Tokyo")
            )
            .flatMap(zone -> Stream.of(
                new CalculateTaskDelayTestCase(zone, LocalDateTime.of(2025, 1, 2, 2, 58, 30), Duration.ofSeconds(90)),
                new CalculateTaskDelayTestCase(zone, LocalDateTime.of(2025, 1, 2, 2, 59, 59), Duration.ofSeconds(1)),
                new CalculateTaskDelayTestCase(zone, LocalDateTime.of(2025, 1, 2, 3, 0, 0), Duration.ofHours(1)),
                new CalculateTaskDelayTestCase(zone, LocalDateTime.of(2025, 1, 2, 3, 0, 1), Duration.ofHours(1).minusSeconds(1)),
                new CalculateTaskDelayTestCase(zone, LocalDateTime.of(2025, 1, 2, 3, 1, 30), Duration.ofHours(1).minusSeconds(90))
            ));
    }
}
