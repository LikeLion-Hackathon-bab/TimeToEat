package com.example.timetoeat.domain.invitation.adap.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvitationRepository extends JpaRepository<InvitationEntity, Long> {
    @Query(""" 
            SELECT i From InvitationEntity i
            JOIN FETCH i.inviter
            WHERE i.invitee.id = :inviteeId AND i.status = com.example.timetoeat.domain.invitation.domain.InvitationStatus.PENDING
            ORDER BY i.id DESC
            """)
    List<InvitationEntity> findInviterByInvitee(@Param("inviteeId") Long inviteeId);

    @Query("""
            select i.id from InvitationEntity i
            where i.status = com.example.timetoeat.domain.invitation.domain.InvitationStatus.APPROVED
              and (i.inviter.id = :viewer or i.invitee.id = :viewer)
              and not exists (select c.invitationId from com.example.timetoeat.domain.plans.meeting.ConfirmedInvitationMeeting c
                              where c.invitationId = i.id)
            order by i.id desc
            """)
    List<Long> findAcceptedRoomIds(@Param("viewer") Long viewer);
    @org.springframework.data.jpa.repository.Modifying
    @Query("""
            update InvitationEntity i set i.status = :status
            where i.id = :id and i.status = com.example.timetoeat.domain.invitation.domain.InvitationStatus.PENDING
            """)
    int resolvePending(@Param("id") Long id,
                       @Param("status") com.example.timetoeat.domain.invitation.domain.InvitationStatus status);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InvitationEntity i join fetch i.inviter join fetch i.invitee where i.id = :id")
    java.util.Optional<InvitationEntity> findForUpdate(@Param("id") Long id);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @Query("""
            update InvitationEntity i set i.status = com.example.timetoeat.domain.invitation.domain.InvitationStatus.CANCELLED
            where i.id = :id and i.status = com.example.timetoeat.domain.invitation.domain.InvitationStatus.APPROVED
            """)
    int cancelApproved(@Param("id") Long id);
}
