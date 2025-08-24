package com.example.timetoeat.domain.invitation.application.port.out;

import com.example.timetoeat.domain.invitation.adap.in.data.InvitationListData;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;

import java.util.List;

public interface LoadInvitationList {
    List<InvitationListData> findInvitationListById(MemberId inviteeId);
}
