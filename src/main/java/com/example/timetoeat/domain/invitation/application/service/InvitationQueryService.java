package com.example.timetoeat.domain.invitation.application.service;

import com.example.timetoeat.domain.invitation.adap.in.data.InvitationListData;
import com.example.timetoeat.domain.invitation.adap.in.dto.InvitationListResponse;
import com.example.timetoeat.domain.invitation.application.port.in.usecase.GetInvitationUseCase;
import com.example.timetoeat.domain.invitation.application.port.out.LoadInvitation;
import com.example.timetoeat.domain.invitation.application.port.out.LoadInvitationList;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvitationQueryService implements GetInvitationUseCase {

    private final LoadInvitationList loadInvitationList;

    @Override
    public List<InvitationListResponse> getInvitationList(MemberId inviteeId) {
        List<InvitationListData> invitationList = loadInvitationList.findInvitationListById(inviteeId);

        return invitationList.stream()
                .map(invitation -> new InvitationListResponse(
                    invitation.invitationId().id(),
                    invitation.inviterName(),
                    invitation.inviterProfileImageUrl()
                ))
                .collect(Collectors.toList());
    }
}
