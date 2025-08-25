package com.example.timetoeat.domain.meal.repository;

import com.example.timetoeat.domain.meal.entity.MemberMealStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberMealStatusRepository extends JpaRepository<MemberMealStatus, Long> {

    Optional<MemberMealStatus> findByMember_Id(Long memberId);

    List<MemberMealStatus> findByMember_IdIn(java.util.Collection<Long> ids);
}