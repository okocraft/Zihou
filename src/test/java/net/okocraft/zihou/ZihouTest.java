package net.okocraft.zihou;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;

class ZihouTest {

    @ParameterizedTest
    @MethodSource("getAdjustedNowTestCases")
    void getAdjustedNow(GetAdjustedNowTestCase testCase) {
        Zihou zihou = new Zihou(Clock.fixed(testCase.now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC), _ -> Component.empty());
        Assertions.assertEquals(testCase.expected, zihou.getAdjustedNow());
    }

    private record GetAdjustedNowTestCase(LocalDateTime now, LocalDateTime expected) {
        @Override
        public String toString() {
            return this.now.toString() + " -> " + this.expected;
        }
    }

    private static Stream<GetAdjustedNowTestCase> getAdjustedNowTestCases() {
        return Stream.of(
            new GetAdjustedNowTestCase(LocalDateTime.of(2025, 1, 2, 2, 59, 59), LocalDateTime.of(2025, 1, 2, 3, 0, 0)),
            new GetAdjustedNowTestCase(LocalDateTime.of(2025, 1, 2, 3, 0, 0), LocalDateTime.of(2025, 1, 2, 3, 0, 0))
        );
    }

    @ParameterizedTest
    @MethodSource("calculateTaskDelayTestCases")
    void calculateTaskDelay(CalculateTaskDelayTestCase testCase) {
        Zihou zihou = new Zihou(Clock.fixed(testCase.now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC), _ -> Component.empty());
        Assertions.assertEquals(testCase.expectedDelay, zihou.calculateTaskDelay());
    }

    private record CalculateTaskDelayTestCase(LocalDateTime now, Duration expectedDelay) {
        @Override
        public String toString() {
            return this.now.toString() + " -> " + this.expectedDelay;
        }
    }

    private static Stream<CalculateTaskDelayTestCase> calculateTaskDelayTestCases() {
        return Stream.of(
            new CalculateTaskDelayTestCase(LocalDateTime.of(2025, 1, 2, 2, 58, 30), Duration.ofSeconds(90)),
            new CalculateTaskDelayTestCase(LocalDateTime.of(2025, 1, 2, 2, 59, 59), Duration.ofSeconds(1)),
            new CalculateTaskDelayTestCase(LocalDateTime.of(2025, 1, 2, 3, 0, 0), Duration.ofHours(1)),
            new CalculateTaskDelayTestCase(LocalDateTime.of(2025, 1, 2, 3, 0, 1), Duration.ofHours(1).minusSeconds(1)),
            new CalculateTaskDelayTestCase(LocalDateTime.of(2025, 1, 2, 3, 1, 30), Duration.ofHours(1).minusSeconds(90))
        );
    }
}
