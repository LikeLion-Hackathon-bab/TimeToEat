package com.example.timetoeat.domain.plans.meeting;

import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

interface MeetingRepository extends JpaRepository<ConfirmedMeeting, Long> {
    @Modifying
    @Query(value = "insert into confirmed_meeting(post_id, restaurant) values (:id, :restaurant) on duplicate key update post_id = post_id", nativeQuery = true)
    void record(@Param("id") long id, @Param("restaurant") String restaurant);

    @Query("select m from ConfirmedMeeting m join fetch m.post p join fetch p.member where " +
           "(p.member.id = :viewer or exists (select a.id from ParticipationEntity a where a.post=p and a.member.id=:viewer)) " +
           "and (:id is null or m.postId=:id) order by p.meetingAt desc, m.postId desc")
    List<ConfirmedMeeting> visible(@Param("viewer") long viewer, @Param("id") Long id);
}
