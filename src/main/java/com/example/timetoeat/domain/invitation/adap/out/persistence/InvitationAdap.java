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
                    .orElseThrow(() -> new IllegalArgumentException("Invalid member id: " + invitation.inviterId().getId()));
            MemberEntity invitee = memberJpaRepository.findById(invitation.inviteeId().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid member id: " + invitation.inviterId().getId()));
            InvitationEntity invitationEntity = mapper.toEntity(invitation, inviter, invitee);
            InvitationEntity saved = invitationRepository.save(invitationEntity);
            return mapper.toDomain(saved);
        } else {
            InvitationEntity invitationEntity = invitationRepository.findById(invitation.inviterId().getId())
                    .orElseThrow(() -> new EntityNotFoundException("not found invitation"));
            mapper.updateEntityFromDomain(invitation,invitationEntity);
            return mapper.toDomain(invitationEntity);
        }
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
}
