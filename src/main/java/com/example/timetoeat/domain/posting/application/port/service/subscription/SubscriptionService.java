package com.example.timetoeat.domain.posting.application.port.service.subscription;

import com.example.timetoeat.domain.posting.application.port.in.usecase.SubscriptionUseCase;
import com.example.timetoeat.domain.posting.application.port.out.save.SubscriptionPort;
import com.example.timetoeat.domain.posting.domain.model.Subscription;
import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import com.example.timetoeat.domain.posting.domain.vo.PostId;
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
        Subscription subscription = Subscription.create(memberId, postId);
        subscriptionPort.save(subscription);
    }
}
