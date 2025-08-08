package com.example.timetoeat.domain.posting.adapter.out.persistence.participation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParticipationJpaRepository extends JpaRepository<ParticipationEntity, Long> {
    // Post의 id를 기준으로 모든 Participation을 찾는 쿼리 메서드
    List<ParticipationEntity> findAllByPostId(Long postId);
    void deleteAllByPostId(Long id);
}
