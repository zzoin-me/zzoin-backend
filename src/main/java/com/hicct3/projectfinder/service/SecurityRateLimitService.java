package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.global.RateLimitException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SecurityRateLimitService {

    private static final Duration STALE_AFTER = Duration.ofHours(24);

    private final Clock clock;
    private final Map<String, WindowState> windows = new ConcurrentHashMap<>();

    public void consume(String action, String subject, int limit, Duration window) {
        WindowState state = state(action, subject);
        synchronized (state) {
            resetExpired(state, window);
            if (state.count >= limit) {
                throw new RateLimitException(retryAfterSeconds(state, window));
            }
            state.count++;
            state.lastTouchedAt = clock.instant();
        }
    }

    public void assertAllowed(String action, String subject, int limit, Duration window) {
        WindowState state = state(action, subject);
        synchronized (state) {
            resetExpired(state, window);
            if (state.count >= limit) {
                throw new RateLimitException(retryAfterSeconds(state, window));
            }
            state.lastTouchedAt = clock.instant();
        }
    }

    public void record(String action, String subject, Duration window) {
        WindowState state = state(action, subject);
        synchronized (state) {
            resetExpired(state, window);
            state.count++;
            state.lastTouchedAt = clock.instant();
        }
    }

    public void reset(String action, String subject) {
        windows.remove(key(action, subject));
    }

    @Scheduled(fixedDelayString = "${app.security.rate-limit-cleanup-ms:600000}")
    public void removeStaleWindows() {
        Instant threshold = clock.instant().minus(STALE_AFTER);
        windows.entrySet().removeIf(entry -> entry.getValue().lastTouchedAt.isBefore(threshold));
    }

    private WindowState state(String action, String subject) {
        return windows.computeIfAbsent(key(action, subject), ignored -> {
            Instant now = clock.instant();
            return new WindowState(now, now);
        });
    }

    private String key(String action, String subject) {
        String safeSubject = subject == null || subject.isBlank() ? "unknown" : subject;
        return action + ':' + safeSubject;
    }

    private void resetExpired(WindowState state, Duration window) {
        Instant now = clock.instant();
        if (!now.isBefore(state.startedAt.plus(window))) {
            state.startedAt = now;
            state.count = 0;
        }
    }

    private long retryAfterSeconds(WindowState state, Duration window) {
        return Math.max(1, Duration.between(clock.instant(), state.startedAt.plus(window)).toSeconds());
    }

    private static final class WindowState {
        private Instant startedAt;
        private Instant lastTouchedAt;
        private int count;

        private WindowState(Instant startedAt, Instant lastTouchedAt) {
            this.startedAt = startedAt;
            this.lastTouchedAt = lastTouchedAt;
        }
    }
}
