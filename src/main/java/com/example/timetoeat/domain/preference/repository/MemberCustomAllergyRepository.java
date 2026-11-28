package com.example.timetoeat.domain.preference.repository;

import com.example.timetoeat.domain.preference.entity.MemberCustomAllergy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberCustomAllergyRepository extends JpaRepository<MemberCustomAllergy, Long> {

    List<MemberCustomAllergy> findByMember_IdOrderBySortOrderAscIdAsc(Long memberId);

    @Modifying(flushAutomatically = true)
    @Query("delete from MemberCustomAllergy a where a.member.id = :memberId")
    int deleteAllOfMember(@Param("memberId") Long memberId);
}
