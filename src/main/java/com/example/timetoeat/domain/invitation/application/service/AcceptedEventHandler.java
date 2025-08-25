package com.example.timetoeat.domain.invitation.application.service;

import com.example.timetoeat.domain.article.service.ArticleMealQueryService;
import com.example.timetoeat.domain.invitation.application.port.out.LoadInvitation;
import com.example.timetoeat.domain.invitation.domain.Invitation;
import com.example.timetoeat.domain.invitation.domain.event.InvitationSentEvent;
import com.example.timetoeat.domain.invitation.domain.vo.InvitationId;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.participation.adap.in.dto.ParticipationSocketDto;
import com.example.timetoeat.domain.participation.application.port.out.LoadParticipation;
import com.example.timetoeat.domain.participation.application.service.ParticipationInfoService;
import com.example.timetoeat.domain.post.adap.in.dto.RecentMenuDto;
import com.example.timetoeat.domain.post.application.service.MatchingPostService;
import com.example.timetoeat.domain.post.application.service.PostParsingService;
import com.example.timetoeat.domain.post.application.service.RecentMenuService;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.global.util.DomainEvent;
import com.example.timetoeat.global.common.EventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AcceptedEventHandler implements EventHandler<InvitationSentEvent> {

    private final LoadParticipation loadParticipation;
    private final ArticleMealQueryService articleMealQueryService;
    private final ParticipationInfoService participationInfoService;
    private final RecentMenuService recentMenuService;
    private final MatchingPostService matchingPostService;
    private final PostParsingService postParsingService;
    private final LoadInvitation loadInvitation;
    private final MemberJpaRepository memberJpaRepository;

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof InvitationSentEvent;
    }

    @Override
    public void handle(InvitationSentEvent event) {
        InvitationId invitationId = event.invitation().invitationId();
        Invitation invitation = loadInvitation.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation not found"));
        MemberId inviterId = invitation.inviterId();
        MemberId inviteeId = invitation.inviteeId();
        Set<MemberId> memberIds = new HashSet<>();
        memberIds.add(inviteeId);
        memberIds.add(inviterId);
        List<ParticipationSocketDto> participations = participationInfoService.getParticipations(memberIds);
        List<RecentMenuDto> recentMenus = recentMenuService.getRecentMenus(memberIds);
        matchingPostService.sendInvitation(invitationId,participations,recentMenus);
    }
}
