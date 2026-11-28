package com.example.timetoeat.domain.post.adap.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PostRepository extends JpaRepository<PostEntity, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PostEntity p where p.id=:id")
    java.util.Optional<PostEntity> findForUpdate(@org.springframework.data.repository.query.Param("id") long id);

    @Query("select p from PostEntity p join fetch p.member where p.status <> com.example.timetoeat.domain.post.domain.Status.OPEN " +
           "and not exists (select m.postId from ConfirmedMeeting m where m.postId=p.id) " +
           "and (p.member.id=:viewer or exists (select a.id from ParticipationEntity a where a.post=p and a.member.id=:viewer)) " +
           "and exists (select a.id from ParticipationEntity a where a.post=p) order by p.id desc")
    List<PostEntity> findWaitingRooms(@org.springframework.data.repository.query.Param("viewer") long viewer);

    /** 신청 가능한 공고만(OPEN이고 만료 전), 최신순. 지연 만료로 OPEN에 남은 공고도 제외한다. */
    @Query("SELECT p FROM PostEntity p JOIN FETCH p.member WHERE p.status = com.example.timetoeat.domain.post.domain.Status.OPEN " +
           "AND p.expiredAt > :now ORDER BY p.createdAt DESC, p.id DESC")
    List<PostEntity> findOpenWithMember(@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);
}
