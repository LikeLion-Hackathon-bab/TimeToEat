package com.example.timetoeat.domain.invitation.application.port.out;

import com.example.timetoeat.domain.invitation.domain.Invitation;

public interface SaveInvitation {
    Invitation save(Invitation invitation);
}
