package com.example.timetoeat.domain.post.application.service;

import com.example.timetoeat.domain.post.application.port.in.usecase.PostUseCase;
import com.example.timetoeat.domain.post.application.port.out.LoadPost;
import com.example.timetoeat.domain.post.application.port.out.PostLock;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 만료 시각이 지난 OPEN 공고를 찾아 마감한다(검증 14 A5).
 * 전에는 누군가 참여를 눌러 410을 받아야만 마감·대기실 생성이 일어났고, 목록은 만료 공고를 숨겨 참여자에게 방이 열리지 않았다.
 *
 * 동시성: 스윕 전체를 Redisson 락 하나로 감싸 인스턴스가 여럿이거나 한 인스턴스에서 겹쳐 불려도 한 번만 돈다.
 * 공고마다 참여 신청과 같은 공고 락(PostLock)을 잡고 PostUseCase.expire()를 부른다. expire()는 행 잠금 뒤
 * status=OPEN·expiredAt<=now 를 다시 확인하므로 410 지연 만료와 겹쳐도 이벤트(대기실 생성)는 한 번만 나간다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostExpirySweeper {
    static final String SWEEP_LOCK = "POST_EXPIRY_SWEEP";
    /** 한 번에 처리할 최대 공고 수. 남으면 다음 주기에 이어서 처리한다. */
    static final int BATCH = 200;

    private final LoadPost loadPost;
    private final PostUseCase postUseCase;
    private final PostLock postLock;
    private final RedissonClient redissonClient;
    private final Clock clock;

    /** @return 이번 스윕에서 마감한 공고 수. 다른 스윕이 도는 중이면 -1. */
    public int sweep() {
        RLock lock = redissonClient.getLock(SWEEP_LOCK);
        boolean locked;
        try {
            locked = lock.tryLock(0, TimeUnit.SECONDS); // 기다리지 않는다. 임대 시간은 watchdog이 연장한다
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
        if (!locked) return -1;
        try {
            List<PostId> due = loadPost.findIdsDueForExpiry(LocalDateTime.now(clock), BATCH);
            int expired = 0;
            for (PostId id : due) {
                try {
                    postLock.lock(id);
                    try {
                        if (postUseCase.expire(id)) expired++;
                    } finally {
                        postLock.unlock(id);
                    }
                } catch (RuntimeException e) {
                    log.warn("공고 만료 처리 실패 postId={} msg={}", id.getId(), e.getMessage(), e);
                }
            }
            if (expired > 0) log.info("만료 공고 {}건 마감", expired);
            return expired;
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }
}
