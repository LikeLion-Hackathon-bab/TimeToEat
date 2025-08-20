package com.example.timetoeat.domain.preference.repository;

import com.example.timetoeat.domain.preference.entity.MemberPreferenceProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberPreferenceProfileRepository extends JpaRepository<MemberPreferenceProfile, Long> {

    Optional<MemberPreferenceProfile> findById(Long memberId);
}
