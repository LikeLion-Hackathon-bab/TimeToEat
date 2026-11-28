package com.example.timetoeat.domain.push.repository;

import com.example.timetoeat.domain.push.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {
    Optional<PushToken> findByToken(String token);
    List<PushToken> findByMemberId(Long memberId);
    void deleteByMemberIdAndToken(Long memberId, String token);

    /** 최근에 등록(또는 다시 등록)한 순서. 상한을 넘긴 오래된 토큰을 고를 때 쓴다. */
    List<PushToken> findByMemberIdOrderByUpdatedAtDescIdDesc(Long memberId);

    /** 같은 토큰을 다시 등록하면 값이 그대로여도 '최근 사용'으로 올린다(감사 필드는 바뀐 값이 있을 때만 갱신됨). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    // DB 시계(CURRENT_TIMESTAMP)가 아니라 감사 필드와 같은 애플리케이션 시계로 쓴다(DB 세션 시간대가 달라도 순서 유지).
    @Query("update PushToken t set t.updatedAt = :now where t.id = :id")
    void touch(@Param("id") Long id, @Param("now") java.time.LocalDateTime now);
}
