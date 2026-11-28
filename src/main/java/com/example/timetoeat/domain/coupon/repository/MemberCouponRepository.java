package com.example.timetoeat.domain.coupon.repository;

import com.example.timetoeat.domain.coupon.entity.MemberCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    List<MemberCoupon> findAllByMember_Id(Long memberId);

    Optional<MemberCoupon> findByIdAndMember_Id(Long id, Long memberId);

    /** 아직 안 쓴 쿠폰만 사용 처리(조건부 UPDATE). 동시 요청 중 정확히 하나만 1을 받는다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update MemberCoupon c set c.usedAt = :now where c.id = :id and c.member.id = :memberId and c.usedAt is null")
    int markUsedIfUnused(@Param("id") Long id, @Param("memberId") Long memberId, @Param("now") LocalDateTime now);
}
