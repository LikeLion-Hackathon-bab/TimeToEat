package com.example.timetoeat.domain.post.adap.in.scheduler;

import com.example.timetoeat.domain.post.application.service.PostExpirySweeper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 1분마다 만료 공고를 마감한다(PostExpirySweeper). post.expiry.scheduler.enabled=false 로 끈다
 * (통합 시험은 끄고 sweep()을 직접 부른다. 다른 시험의 공고를 몰래 마감하지 않게).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "post.expiry.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PostExpiryScheduler {
    private final PostExpirySweeper sweeper;

    @Scheduled(fixedDelayString = "${post.expiry.scheduler.fixed-delay-ms:60000}",
               initialDelayString = "${post.expiry.scheduler.initial-delay-ms:30000}")
    public void run() {
        sweeper.sweep();
    }
}
