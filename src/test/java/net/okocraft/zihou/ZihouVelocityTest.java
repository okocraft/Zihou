package net.okocraft.zihou;

import com.velocitypowered.api.scheduler.ScheduledTask;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

class ZihouVelocityTest {

    @Test
    void createRuntimeStateKeepsConfigAndClockTogether() {
        ZihouConfig config = new ZihouConfig("message", "Asia/Tokyo");

        ZihouVelocity.RuntimeState state = ZihouVelocity.createRuntimeState(config);

        Assertions.assertSame(config, state.config());
        Assertions.assertEquals(ZoneId.of("Asia/Tokyo"), state.clock().getZone());
    }

    @Test
    void createRuntimeStateFallsBackForInvalidTimezone() {
        ZihouConfig config = new ZihouConfig("message", "invalid");

        ZihouVelocity.RuntimeState state = ZihouVelocity.createRuntimeState(config);

        Assertions.assertSame(config, state.config());
        Assertions.assertEquals(ZoneId.systemDefault(), state.clock().getZone());
    }

    @Test
    void replaceTaskPublishesNewTaskAndCancelsPreviousTask() {
        AtomicBoolean cancelled = new AtomicBoolean();
        ScheduledTask previous = scheduledTask(cancelled);
        ScheduledTask replacement = scheduledTask(new AtomicBoolean());
        AtomicReference<ScheduledTask> taskReference = new AtomicReference<>(previous);

        ZihouVelocity.replaceTask(taskReference, replacement);

        Assertions.assertSame(replacement, taskReference.get());
        Assertions.assertTrue(cancelled.get());
    }

    private static ScheduledTask scheduledTask(AtomicBoolean cancelled) {
        return (ScheduledTask) Proxy.newProxyInstance(
            ScheduledTask.class.getClassLoader(),
            new Class<?>[]{ScheduledTask.class},
            (proxy, method, args) -> {
                if (method.getName().equals("cancel")) {
                    cancelled.set(true);
                }
                return null;
            }
        );
    }

    @ParameterizedTest
    @MethodSource("getAdjustedNowTestCases")
    void getAdjustedNow(GetAdjustedNowTestCase testCase) {
        Assertions.assertEquals(testCase.expected, ZihouVelocity.getAdjustedNow(Clock.fixed(testCase.now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)));
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
        Assertions.assertEquals(
            testCase.expectedDelay,
            ZihouVelocity.calculateTaskDelay(Clock.fixed(testCase.now.atZone(testCase.zoneId).toInstant(), testCase.zoneId))
        );
    }

    private record CalculateTaskDelayTestCase(LocalDateTime now, ZoneId zoneId, Duration expectedDelay) {
        @Override
        public String toString() {
            return this.now.toString() + " -> " + this.expectedDelay;
        }
    }

    private static Stream<CalculateTaskDelayTestCase> calculateTaskDelayTestCases() {
        return Stream.of(
            new CalculateTaskDelayTestCase(LocalDateTime.of(2025, 1, 2, 2, 58, 30), ZoneOffset.UTC, Duration.ofSeconds(90)),
            new CalculateTaskDelayTestCase(LocalDateTime.of(2025, 1, 2, 2, 59, 59), ZoneOffset.UTC, Duration.ofSeconds(1)),
            new CalculateTaskDelayTestCase(LocalDateTime.of(2025, 1, 2, 3, 0, 0), ZoneOffset.UTC, Duration.ofHours(1)),
            new CalculateTaskDelayTestCase(LocalDateTime.of(2025, 1, 2, 3, 0, 1), ZoneOffset.UTC, Duration.ofHours(1).minusSeconds(1)),
            new CalculateTaskDelayTestCase(LocalDateTime.of(2025, 1, 2, 3, 1, 30), ZoneOffset.UTC, Duration.ofHours(1).minusSeconds(90)),
            new CalculateTaskDelayTestCase(LocalDateTime.of(2025, 1, 2, 3, 30, 0), ZoneId.of("Asia/Kolkata"), Duration.ofMinutes(30)),
            new CalculateTaskDelayTestCase(LocalDateTime.of(2025, 1, 2, 3, 45, 0), ZoneId.of("Asia/Kathmandu"), Duration.ofMinutes(15))
        );
    }

    @ParameterizedTest
    @MethodSource("daylightSavingTransitionTestCases")
    void calculateTaskDelayAcrossDaylightSavingTransition(DaylightSavingTransitionTestCase testCase) {
        Clock clock = Clock.fixed(testCase.now, testCase.zoneId);

        Assertions.assertEquals(testCase.expectedDelay, ZihouVelocity.calculateTaskDelay(clock));
    }

    private record DaylightSavingTransitionTestCase(Instant now, ZoneId zoneId, Duration expectedDelay) {
        @Override
        public String toString() {
            return this.now + " in " + this.zoneId;
        }
    }

    private static Stream<DaylightSavingTransitionTestCase> daylightSavingTransitionTestCases() {
        ZoneId newYork = ZoneId.of("America/New_York");
        return Stream.of(
            new DaylightSavingTransitionTestCase(Instant.parse("2025-03-09T06:30:00Z"), newYork, Duration.ofMinutes(30)),
            new DaylightSavingTransitionTestCase(Instant.parse("2025-11-02T05:30:00Z"), newYork, Duration.ofMinutes(30))
        );
    }
}
