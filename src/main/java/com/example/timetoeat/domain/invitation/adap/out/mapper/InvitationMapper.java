package com.example.timetoeat.domain.invitation.adap.out.mapper;

import com.example.timetoeat.domain.invitation.adap.in.data.InvitationListData;
import com.example.timetoeat.domain.invitation.adap.out.persistence.InvitationEntity;
import com.example.timetoeat.domain.invitation.domain.Invitation;
import com.example.timetoeat.domain.invitation.domain.vo.InvitationId;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface InvitationMapper {

    @Mapping(source = "id", target = "invitationId", qualifiedByName = "longToInvitationId")
    @Mapping(source = "inviter.id", target = "inviterId", qualifiedByName = "longToMemberId")
    @Mapping(source = "invitee.id", target = "inviteeId", qualifiedByName = "longToMemberId")
    Invitation toDomain(InvitationEntity invitationEntity);

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "inviter", target = "inviter")
    @Mapping(source = "invitee", target = "invitee")
    @Mapping(source = "invitation.message", target = "message")
    @Mapping(source = "invitation.status", target = "status")
    @Mapping(source = "invitation.createdAt", target = "createdAt")
    InvitationEntity toEntity(Invitation invitation, MemberEntity inviter, MemberEntity invitee);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "inviter", ignore = true)
    @Mapping(target = "invitee", ignore = true)
    void updateEntityFromDomain(Invitation invitation, @MappingTarget InvitationEntity invitationEntity);

    @Mapping(source = "id", target = "invitationId", qualifiedByName = "longToInvitationId")
    @Mapping(source = "inviter.username", target = "inviterName")
    @Mapping(source = "inviter.profileImageUrl", target = "inviterProfileImageUrl")
    InvitationListData toInvitationListData(InvitationEntity invitationEntity);

    @Named("longToInvitationId")
    default InvitationId longToInvitationId(Long id) {
        return id != null ? new InvitationId(id) : null;
    }

    @Named("longToMemberId")
    default MemberId longToMemberId(Long id) {
        return id != null ? new MemberId(id) : null;
    }
}
