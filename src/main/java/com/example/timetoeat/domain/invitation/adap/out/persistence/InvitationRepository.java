package com.example.timetoeat.domain.invitation.adap.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvitationRepository extends JpaRepository<InvitationEntity, Long> {
    @Query(""" 
            SELECT i From InvitationEntity i
            JOIN FETCH i.inviter
            WHERE i.invitee.id = :inviteeId
            """)
    List<InvitationEntity> findInviterByInvitee(@Param("inviteeId") Long inviteeId);
}
