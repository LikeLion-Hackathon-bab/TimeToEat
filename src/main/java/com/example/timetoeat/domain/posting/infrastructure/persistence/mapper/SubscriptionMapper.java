package com.example.timetoeat.domain.posting.infrastructure.persistence.mapper;

import com.example.timetoeat.domain.posting.infrastructure.persistence.post.PostEntity;
import com.example.timetoeat.domain.posting.infrastructure.persistence.subscription.SubscriptionEntity;
import com.example.timetoeat.domain.posting.core.domain.model.subscription.Subscription;
import com.example.timetoeat.domain.posting.core.domain.vo.member.MemberId;
import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {
    @Mapping(source = "member.id", target = "memberId", qualifiedByName = "longToMemberId")
    @Mapping(source = "post.id", target = "postId", qualifiedByName = "longToPostId")
    Subscription toDomain(SubscriptionEntity subscriptionEntity);

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "postEntity", target = "post")
    @Mapping(source = "memberEntity", target = "member")
    SubscriptionEntity toEntity(Subscription subscription, PostEntity postEntity, MemberEntity memberEntity);

    @Named("longToPostId")
    default PostId longToPostId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Post ID는 null일 수 없습니다.");
        }
        return new PostId(id);
    }

    @Named("longToMemberId")
    default MemberId longToMemberId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Member ID는 null일 수 없습니다.");
        }
        return new MemberId(id);
    }
}
