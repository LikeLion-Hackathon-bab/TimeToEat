package com.example.timetoeat.domain.invitation.application.service;

import com.example.timetoeat.domain.invitation.application.port.out.LoadInvitation;
import com.example.timetoeat.domain.invitation.domain.Invitation;
import com.example.timetoeat.domain.invitation.domain.InvitationStatus;
import com.example.timetoeat.domain.invitation.domain.vo.InvitationId;
import com.example.timetoeat.domain.participation.application.service.ParticipationInfoService;
import com.example.timetoeat.domain.post.application.service.MatchingPostService;
import com.example.timetoeat.domain.post.application.service.RecentMenuService;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvitationRoomService {
    private final LoadInvitation invitations;
    private final ParticipationInfoService profiles;
    private final RecentMenuService menus;
    private final MatchingPostService socket;

    public void enter(Long viewer, long id) {
        if (viewer == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var person = new MemberId(viewer);
        var invitation = invitations.findById(new InvitationId(id))
                .filter(i -> i.status() == InvitationStatus.APPROVED &&
                        (person.equals(i.inviterId()) || person.equals(i.inviteeId())))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        prepare(invitation);
    }

    void prepare(Invitation invitation) {
        var members = new LinkedHashSet<>(List.of(invitation.inviterId(), invitation.inviteeId()));
        var people = profiles.getParticipations(members);
        var recent = menus.getRecentMenus(members);
        try {
            socket.ensureInvitation(invitation.invitationId(), people, recent);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "대기실 연결을 다시 시도해 주세요.", e);
        }
    }
}
