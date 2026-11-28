package com.example.timetoeat.domain.referral.repository;

import com.example.timetoeat.domain.referral.entity.ReferralCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReferralCodeRepository extends JpaRepository<ReferralCode, Long> {

    boolean existsByCode(String code);

    Optional<ReferralCode> findByCode(String code);

    /** 이 회원이 이미 추천 코드를 등록했는지(회원당 1회) */
    boolean existsByUsedBy_Id(Long memberId);

    List<ReferralCode> findAllByInviter_IdOrderByCreatedAtDesc(Long inviterId);

    /**
     * 친구 관계: 추천 코드를 수령(used_by 채워짐)한 순간 초대한 사람과 수령한 사람은 서로 친구다(양방향, 수락 = 수령).
     * 앱에 별도 친구 테이블이 없고, 초대 링크 수령이 유일한 친구 맺기 경로다.
     */
    @Query("select r.usedBy.id from ReferralCode r where r.inviter.id = :me and r.usedBy is not null")
    List<Long> findInviteeIdsOf(@Param("me") Long memberId);

    @Query("select r.inviter.id from ReferralCode r where r.usedBy.id = :me")
    List<Long> findInviterIdsOf(@Param("me") Long memberId);

    default java.util.Set<Long> findFriendIds(Long memberId) {
        var ids = new java.util.HashSet<Long>(findInviteeIdsOf(memberId));
        ids.addAll(findInviterIdsOf(memberId));
        ids.remove(memberId);
        return ids;
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReferralCode r where r.code = :code")
    Optional<ReferralCode> findForUpdateByCode(@Param("code") String code);
}
