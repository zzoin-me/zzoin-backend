package com.hicct3.projectfinder.global;

import lombok.Getter;

@Getter
public class RateLimitException extends GeneralException {
    private final long retryAfterSeconds;

    public RateLimitException(long retryAfterSeconds) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }
}
