package com.example.timetoeat.domain.posting.infrastructure.persistence.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {

    @Query("SELECT COUNT(S) > 0 FROM SubscriptionEntity S WHERE S.member.id = :memberId AND S.post.id = :postId")
    boolean checkSubscriptionExist(@Param("memberId") Long memberId, @Param("postId") Long postId);

    @Query("SELECT S.member.id FROM SubscriptionEntity S WHERE S.post.id = :postId")
    List<Long> findMemberIdsByPostId(@Param("postId") Long postId);
}
