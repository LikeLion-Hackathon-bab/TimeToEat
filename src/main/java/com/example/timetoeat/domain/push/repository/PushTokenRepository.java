package com.example.timetoeat.domain.push.repository;

import com.example.timetoeat.domain.push.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {
    Optional<PushToken> findByToken(String token);
    List<PushToken> findByMemberId(Long memberId);
    void deleteByMemberIdAndToken(Long memberId, String token);
}
