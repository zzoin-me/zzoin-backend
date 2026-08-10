package com.hicct3.projectfinder.global.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class TimeConfig {

    private final ZoneId zoneId;

    public TimeConfig(@Value("${app.time-zone:Asia/Seoul}") String timeZone) {
        this.zoneId = ZoneId.of(timeZone);
    }

    @PostConstruct
    public void configureDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
    }

    @Bean
    public Clock appClock() {
        return Clock.system(zoneId);
    }
}
