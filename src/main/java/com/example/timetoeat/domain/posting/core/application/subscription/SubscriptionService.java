package com.example.timetoeat.domain.posting.core.application.subscription;

import com.example.timetoeat.domain.posting.core.application.port.out.gateway.subscription.SubscriptionPort;
import com.example.timetoeat.domain.posting.core.application.port.in.usecase.usecase.subscription.SubscriptionUseCase;
import com.example.timetoeat.domain.posting.core.domain.model.subscription.Subscription;
import com.example.timetoeat.domain.posting.core.domain.vo.member.MemberId;
import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubscriptionService implements SubscriptionUseCase {

    private final SubscriptionPort subscriptionPort;

    @Transactional
    @Override
    public void subscribe(MemberId memberId, PostId postId) {
        if (subscriptionPort.exists(memberId, postId)) {
            throw new IllegalStateException("이미 알림을 구독한 공고입니다.");
        }
        Subscription subscription = new Subscription(memberId, postId);
        subscriptionPort.save(subscription);
    }
}
