package com.example.timetoeat.domain.invitation.adap.out.persistence;

import com.example.timetoeat.domain.invitation.domain.Invitation;
import com.example.timetoeat.domain.invitation.domain.InvitationStatus;
import com.example.timetoeat.domain.invitation.domain.vo.InvitationId;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvitationAdapTest {
    @Test
    void updatesTheSelectedInvitationOnlyOnce() {
        var repo = mock(InvitationRepository.class);
        var adapter = new InvitationAdap(repo, null, null);
        var accepted = new Invitation(new InvitationId(37L), new MemberId(1L), new MemberId(2L),
                "점심", InvitationStatus.APPROVED, LocalDateTime.now());
        when(repo.resolvePending(37L, InvitationStatus.APPROVED)).thenReturn(1, 0);
        assertEquals(accepted, adapter.save(accepted));
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> adapter.save(accepted)).getStatusCode().value());
    }
}
