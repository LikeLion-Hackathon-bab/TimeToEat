package com.example.timetoeat.domain.posting.infrastructure.persistence.subscription;

import com.example.timetoeat.domain.posting.core.application.gateway.subscription.GetSubscriptionPort;
import com.example.timetoeat.domain.posting.core.application.gateway.subscription.SubscriptionPort;
import com.example.timetoeat.domain.posting.infrastructure.mapper.SubscriptionMapper;
import com.example.timetoeat.domain.posting.infrastructure.persistence.post.PostEntity;
import com.example.timetoeat.domain.posting.infrastructure.persistence.post.PostJpaRepository;
import com.example.timetoeat.domain.posting.core.domain.model.subscription.Subscription;
import com.example.timetoeat.domain.posting.core.domain.vo.member.MemberId;
import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SubscriptionPersistenceAdapter implements SubscriptionPort, GetSubscriptionPort {

    private final SubscriptionJpaRepository subscriptionJpaRepository;
    private final PostJpaRepository postJpaRepository;
    private final MemberJpaRepository memberJpaRepository;
    private final SubscriptionMapper subscriptionMapper;

    @Override
    public void save(Subscription subscription) {
        Long memberId = subscription.memberId().getId();
        Long postId = subscription.postId().getId();

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

    @Override
    public List<MemberId> findMembersByPostId(PostId postId) {
        List<Long> memberIds = subscriptionJpaRepository.findMemberIdsByPostId(postId.getId());
        return memberIds.stream()
                .map(memberId -> new MemberId(memberId))
                .collect(Collectors.toList());
    }
}
