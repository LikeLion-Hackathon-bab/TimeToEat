package com.example.timetoeat.domain.invitation.adap.out.persistence;

import com.example.timetoeat.domain.invitation.adap.in.data.InvitationListData;
import com.example.timetoeat.domain.invitation.adap.out.mapper.InvitationMapper;
import com.example.timetoeat.domain.invitation.application.port.out.LoadInvitation;
import com.example.timetoeat.domain.invitation.application.port.out.LoadInvitationList;
import com.example.timetoeat.domain.invitation.application.port.out.SaveInvitation;
import com.example.timetoeat.domain.invitation.domain.Invitation;
import com.example.timetoeat.domain.invitation.domain.vo.InvitationId;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InvitationAdap implements LoadInvitation, SaveInvitation, LoadInvitationList {

    private final InvitationRepository invitationRepository;
    private final MemberJpaRepository memberJpaRepository;
    private final InvitationMapper mapper;

    @Override
    public Invitation save(Invitation invitation) {
        if (invitation.invitationId() == null) {
            MemberEntity inviter = memberJpaRepository.findById(invitation.inviterId().getId())
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.UNAUTHORIZED, "보내는 회원을 찾을 수 없습니다."));
            MemberEntity invitee = memberJpaRepository.findById(invitation.inviteeId().getId())
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.NOT_FOUND, "초대할 회원을 찾을 수 없습니다."));
            InvitationEntity invitationEntity = mapper.toEntity(invitation, inviter, invitee);
            InvitationEntity saved = invitationRepository.save(invitationEntity);
            return mapper.toDomain(saved);
        }
        if (invitationRepository.resolvePending(invitation.invitationId().id(), invitation.status()) != 1) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "이미 처리된 초대입니다.");
        }
        return invitation;
    }

    @Override
    public Optional<Invitation> findById(InvitationId invitationId) {
        return invitationRepository.findById(invitationId.id())
                .map(mapper::toDomain);
    }


    @Override
    public List<InvitationListData> findInvitationListById(MemberId inviteeId) {
        List<InvitationEntity> inviters = invitationRepository.findInviterByInvitee(inviteeId.getId());
        return inviters.stream()
                .map(mapper::toInvitationListData)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getAcceptedRoomIds(MemberId viewer) {
        return invitationRepository.findAcceptedRoomIds(viewer.getId());
    }
}
