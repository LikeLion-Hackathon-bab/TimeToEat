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

    @Query("SELECT p FROM PostEntity p JOIN FETCH p.member")
    List<PostEntity> findAllWithMember();
}
