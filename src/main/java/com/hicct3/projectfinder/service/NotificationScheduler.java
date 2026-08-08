package com.hicct3.projectfinder.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @deprecated 마감일 체크는 ProjectStatusScheduler (5분 주기)로 이관됨.
 *             향후 다른 스케줄링 작업이 필요하면 이 클래스에 추가.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationScheduler {

    // 기존 deadline 체크 로직은 ProjectStatusScheduler.autoCloseExpiredRecruitments()로 이관됨
}
