package com.example.timetoeat.domain.subscription.application.service;

import com.example.timetoeat.domain.subscription.application.port.out.SaveSubscription;
import com.example.timetoeat.domain.subscription.application.port.in.usecase.SubscriptionUseCase;
import com.example.timetoeat.domain.subscription.domain.Subscription;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService implements SubscriptionUseCase {

    private final SaveSubscription saveSubscription;

    @Transactional
    @Override
    public void subscribe(MemberId memberId, PostId postId) {
        if (saveSubscription.exists(memberId, postId)) {
            throw new IllegalStateException("이미 알림을 구독한 공고입니다.");
        }
        Subscription subscription = new Subscription(memberId, postId);
        saveSubscription.save(subscription);
    }
}
