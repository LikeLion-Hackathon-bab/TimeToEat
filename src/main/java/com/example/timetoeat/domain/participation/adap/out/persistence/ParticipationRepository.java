package com.example.timetoeat.domain.participation.adap.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ParticipationRepository extends JpaRepository<ParticipationEntity, Long> {
    List<ParticipationEntity> findAllByPostId(Long postId);

    void deleteAllByPostId(Long id);

    /** 대기실 나가기(F46): 본인 참여 행 하나를 지운다. 지운 행 수를 돌려준다. */
    @org.springframework.data.jpa.repository.Modifying
    @Query("delete from ParticipationEntity p where p.post.id = :postId and p.member.id = :memberId")
    int deleteByPostAndMember(@org.springframework.data.repository.query.Param("postId") Long postId,
                              @org.springframework.data.repository.query.Param("memberId") Long memberId);

    @Query("SELECT P FROM ParticipationEntity P JOIN FETCH P.member WHERE P.post.id IN :postIds")
    List<ParticipationEntity> findMembersByPost(List<Long> postIds);
}


