package com.hicct3.projectfinder.service;

import com.hicct3.projectfinder.repository.PostViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PostViewCleanupService {
    private final PostViewRepository postViewRepository;
    private final Clock clock;

    @Scheduled(cron = "0 30 4 * * *", zone = "${app.time-zone:Asia/Seoul}")
    @Transactional
    public void deleteExpiredViewKeys() {
        postViewRepository.deleteByViewedHourBefore(LocalDateTime.now(clock).minusDays(90));
    }
}
