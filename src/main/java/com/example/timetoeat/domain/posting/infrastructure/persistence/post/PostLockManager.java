package com.example.timetoeat.domain.posting.infrastructure.persistence.post;

import com.example.timetoeat.domain.posting.core.application.port.out.gateway.post.PostLock;
import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class PostLockManager implements PostLock {

    private final RedissonClient redissonClient;
    private final ThreadLocal<RLock> lockHolder = new ThreadLocal<>();

    @Override
    public void lock(PostId postId) {
        RLock lock = redissonClient.getLock(getLockKey(postId));

        try {
            boolean available = lock.tryLock(4, 30, TimeUnit.SECONDS);
            if (!available) {
                throw new IllegalStateException("락을 획득할 수 없습니다.");
            }
            lockHolder.set(lock);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 획득 중 인터럽트 발생", e);
        }
    }

    @Override
    public void unlock(PostId postId) {
        RLock lock = lockHolder.get();
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            lockHolder.remove();
        }
    }

    private String getLockKey(PostId postId) {
        return "POST_LOCK:" + postId.getId();
    }
}

