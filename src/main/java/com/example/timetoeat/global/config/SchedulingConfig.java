package com.example.timetoeat.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** @Scheduled 작업(공고 만료 PostExpiryScheduler 등)을 켠다. 개별 작업은 각자의 속성으로 끈다. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
