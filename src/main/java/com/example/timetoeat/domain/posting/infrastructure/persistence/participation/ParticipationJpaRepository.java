package com.example.timetoeat.domain.posting.infrastructure.persistence.participation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ParticipationJpaRepository extends JpaRepository<ParticipationEntity, Long> {
    List<ParticipationEntity> findAllByPostId(Long postId);

    void deleteAllByPostId(Long id);

    @Query("SELECT P FROM ParticipationEntity P JOIN FETCH P.member WHERE P.post.id IN :postIds")
    List<ParticipationEntity> findMembersByPost(List<Long> postIds);
}
