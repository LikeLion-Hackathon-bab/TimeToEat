package com.example.timetoeat.domain.invitation.application.service;

import com.example.timetoeat.domain.invitation.application.port.out.LoadInvitation;
import com.example.timetoeat.domain.invitation.domain.Invitation;
import com.example.timetoeat.domain.invitation.domain.InvitationStatus;
import com.example.timetoeat.domain.invitation.domain.vo.InvitationId;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvitationCommandServiceTest {
    @Test
    void retryAfterApprovalCannotChangeTheDecision() {
        var loader = mock(LoadInvitation.class);
        var service = new InvitationCommandService(null, loader, null);
        var recipient = new MemberId(2L);
        var id = new InvitationId(37L);
        when(loader.findById(id)).thenReturn(Optional.of(new Invitation(id, new MemberId(1L),
                recipient, "점심", InvitationStatus.APPROVED, LocalDateTime.now())));
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.acceptInvitation(recipient, id)).getStatusCode().value());
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> service.rejectInvitation(recipient, id)).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.acceptInvitation(new MemberId(3L), id)).getStatusCode().value());
    }
}
