package com.example.timetoeat.domain.notification.listener;

import com.example.timetoeat.domain.notification.service.NotificationTriggerRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 식사 약속 시간 도래 알림(§14.2 MEETING_REMINDER). 1분마다 만남 30분 전 안쪽의 확정 약속을 찾는다.
 * 서버가 여러 대이거나 재시작으로 겹쳐 돌아도 dedup_key 유니크라 사람마다 한 번만 간다.
 * 끄기: notification.reminder.scheduler.enabled=false (통합 시험은 끄고 remindUpcoming()을 직접 부른다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.reminder.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class MeetingReminderScheduler {

    private final NotificationTriggerRecorder recorder;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void run() {
        try {
            int n = recorder.remindUpcoming();
            if (n > 0) log.debug("meeting reminders checked receivers={}", n);
        } catch (RuntimeException e) {
            log.warn("meeting reminder failed msg={}", e.getMessage());
        }
    }
}
