package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.global.RateLimitException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityRateLimitServiceTest {

    @Test
    void blocksAfterLimitAndAllowsAfterWindowExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-11T00:00:00Z"));
        SecurityRateLimitService service = new SecurityRateLimitService(clock);

        service.consume("email", "user@example.com", 1, Duration.ofSeconds(60));
        assertThrows(
                RateLimitException.class,
                () -> service.consume("email", "user@example.com", 1, Duration.ofSeconds(60)));

        clock.advance(Duration.ofSeconds(60));
        assertDoesNotThrow(
                () -> service.consume("email", "user@example.com", 1, Duration.ofSeconds(60)));
    }

    @Test
    void recordedFailuresCanBeResetAfterSuccessfulLogin() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-11T00:00:00Z"));
        SecurityRateLimitService service = new SecurityRateLimitService(clock);

        for (int i = 0; i < 5; i++) {
            service.record("login", "ip|email", Duration.ofMinutes(15));
        }
        assertThrows(
                RateLimitException.class,
                () -> service.assertAllowed("login", "ip|email", 5, Duration.ofMinutes(15)));

        service.reset("login", "ip|email");
        assertDoesNotThrow(
                () -> service.assertAllowed("login", "ip|email", 5, Duration.ofMinutes(15)));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
