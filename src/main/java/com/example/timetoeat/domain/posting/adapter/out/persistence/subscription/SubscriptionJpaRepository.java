package com.example.timetoeat.domain.posting.adapter.out.persistence.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionEntity, Long> {

    @Query("SELECT COUNT(S) > 0 FROM SubscriptionEntity S WHERE S.member.id = :memberId AND s.post.id = :postId")
    Boolean checkSubscriptionExist(@Param("memberId") Long memberId, @Param("postId") Long postId);
}
