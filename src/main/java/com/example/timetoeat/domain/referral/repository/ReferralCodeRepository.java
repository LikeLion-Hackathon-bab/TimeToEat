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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReferralCode r where r.code = :code")
    Optional<ReferralCode> findForUpdateByCode(@Param("code") String code);
}
