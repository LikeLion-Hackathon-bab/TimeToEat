package com.example.timetoeat.domain.posting.adapter.out.persistence.subscription;

import com.example.timetoeat.domain.posting.adapter.out.mapper.SubscriptionMapper;
import com.example.timetoeat.domain.posting.adapter.out.persistence.post.PostEntity;
import com.example.timetoeat.domain.posting.adapter.out.persistence.post.PostJpaRepository;
import com.example.timetoeat.domain.posting.application.port.out.save.SubscriptionPort;
import com.example.timetoeat.domain.posting.domain.model.Subscription;
import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import com.example.timetoeat.domain.posting.domain.vo.PostId;
import com.example.timetoeat.global.auth.entity.MemberEntity;
import com.example.timetoeat.global.auth.repository.MemberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionPersistenceAdapter implements SubscriptionPort {

    private final SubscriptionJpaRepository subscriptionJpaRepository;
    private final PostJpaRepository postJpaRepository;
    private final MemberJpaRepository memberJpaRepository;
    private final SubscriptionMapper subscriptionMapper;


    @Override
    public void save(Subscription subscription) {
        Long memberId = subscription.getMemberId().getId();
        Long postId = subscription.getPostId().getId();

        MemberEntity memberEntity = memberJpaRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        PostEntity postEntity = postJpaRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공고입니다."));

        SubscriptionEntity subscriptionEntity = subscriptionMapper.toEntity(subscription, postEntity, memberEntity);
        subscriptionJpaRepository.save(subscriptionEntity);
    }

    @Override
    public boolean exists(MemberId memberId, PostId postId) {
        return subscriptionJpaRepository.checkSubscriptionExist(memberId.getId(), postId.getId());
    }
}
