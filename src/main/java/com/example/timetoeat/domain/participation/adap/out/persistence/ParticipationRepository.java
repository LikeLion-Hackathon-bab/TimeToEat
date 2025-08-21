package com.example.timetoeat.domain.participation.adap.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ParticipationRepository extends JpaRepository<ParticipationEntity, Long> {
    List<ParticipationEntity> findAllByPostId(Long postId);

    void deleteAllByPostId(Long id);

    @Query("SELECT P FROM ParticipationEntity P JOIN FETCH P.member WHERE P.post.id IN :postIds")
    List<ParticipationEntity> findMembersByPost(List<Long> postIds);
}


