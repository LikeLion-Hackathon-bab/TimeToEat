package com.example.timetoeat.domain.plans.meeting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ConfirmedInvitationRepository extends JpaRepository<ConfirmedInvitationMeeting, Long> {
    /** 첫 저장만 반영(재전송은 기존 행 유지). 같은 값인지는 호출자가 다시 읽어 비교한다. */
    @Modifying
    @Query(value = "insert into confirmed_invitation_meeting(invitation_id, meeting_at, location, restaurant) " +
            "values (:id, :meetingAt, :location, :restaurant) on duplicate key update invitation_id = invitation_id",
            nativeQuery = true)
    void record(@Param("id") long id, @Param("meetingAt") LocalDateTime meetingAt,
                @Param("location") String location, @Param("restaurant") String restaurant);

    @Query("select m from ConfirmedInvitationMeeting m join fetch m.invitation i join fetch i.inviter join fetch i.invitee " +
           "where (i.inviter.id = :viewer or i.invitee.id = :viewer) and (:id is null or m.invitationId = :id) " +
           "order by m.meetingAt desc, m.invitationId desc")
    List<ConfirmedInvitationMeeting> visible(@Param("viewer") long viewer, @Param("id") Long id);
}
