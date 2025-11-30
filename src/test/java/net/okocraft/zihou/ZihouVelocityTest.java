package net.okocraft.zihou;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;

class ZihouVelocityTest {

    @Test
    void getAdjustedNow() {
        LocalDateTime now = LocalDateTime.of(2025, 1, 2, 3, 0, 0);

        // return as-is
        Assertions.assertEquals(
                LocalDateTime.of(2025, 1, 2, 3, 0, 0),
                ZihouVelocity.getAdjustedNow(Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC))
        );

        // return adjusted
        Assertions.assertEquals(
                LocalDateTime.of(2025, 1, 2, 3, 0, 0),
                ZihouVelocity.getAdjustedNow(Clock.fixed(now.minusSeconds(1).toInstant(ZoneOffset.UTC), ZoneOffset.UTC))
        );
    }

    @ParameterizedTest
    @MethodSource("calculateTaskDelayTestCases")
    void calculateTaskDelay(CalculateTaskDelayTestCase testCase) {
        Assertions.assertEquals(
                testCase.expectedDelay,
                ZihouVelocity.calculateTaskDelay(Clock.fixed(testCase.now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC))
        );
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
