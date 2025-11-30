package net.okocraft.zihou;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

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

}
