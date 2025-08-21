package com.example.timetoeat.domain.subscription.adap.out.persistence;

import com.example.timetoeat.domain.subscription.application.port.out.LoadSubscription;
import com.example.timetoeat.domain.subscription.application.port.out.SaveSubscription;
import com.example.timetoeat.domain.subscription.adap.out.mapper.SubscriptionMapper;
import com.example.timetoeat.domain.post.adap.out.persistence.PostEntity;
import com.example.timetoeat.domain.post.adap.out.persistence.PostRepository;
import com.example.timetoeat.domain.subscription.domain.Subscription;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SubscriptionAdap implements SaveSubscription, LoadSubscription {

    private final SubscriptionRepository subscriptionRepository;
    private final PostRepository postRepository;
    private final MemberJpaRepository memberJpaRepository;
    private final SubscriptionMapper subscriptionMapper;

    @Override
    public void save(Subscription subscription) {
        Long memberId = subscription.memberId().getId();
        Long postId = subscription.postId().getId();

        MemberEntity memberEntity = memberJpaRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));
        PostEntity postEntity = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공고입니다."));

        SubscriptionEntity subscriptionEntity = subscriptionMapper.toEntity(subscription, postEntity, memberEntity);
        subscriptionRepository.save(subscriptionEntity);
    }

    @Override
    public boolean exists(MemberId memberId, PostId postId) {
        return subscriptionRepository.checkSubscriptionExist(memberId.getId(), postId.getId());
    }

    @Override
    public List<MemberId> findMembersByPostId(PostId postId) {
        List<Long> memberIds = subscriptionRepository.findMemberIdsByPostId(postId.getId());
        return memberIds.stream()
                .map(memberId -> new MemberId(memberId))
                .collect(Collectors.toList());
    }
}
