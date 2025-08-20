package com.example.timetoeat.domain.preference.repository;

import com.example.timetoeat.domain.preference.entity.MemberFoodPreference;
import com.example.timetoeat.domain.preference.entity.PreferenceType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberFoodPreferenceRepository extends JpaRepository<MemberFoodPreference, Long> {

    @EntityGraph(attributePaths = "foodCode")
    List<MemberFoodPreference> findByMember_IdAndType(Long memberId, PreferenceType type);

    void deleteByMember_IdAndType(Long memberId, PreferenceType type);

    boolean existsByMember_IdAndFoodCode_CodeAndType(Long memberId, String code, PreferenceType type);
}
