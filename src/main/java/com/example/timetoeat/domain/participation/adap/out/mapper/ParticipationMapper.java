package com.example.timetoeat.domain.participation.adap.out.mapper;

import com.example.timetoeat.domain.participation.adap.out.persistence.ParticipationEntity;
import com.example.timetoeat.domain.participation.domain.Participation;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Mapper(componentModel = "spring")
public interface ParticipationMapper {
    @Mapping(source = "entities", target = "memberIds")
    Participation toDomain(PostId postId, List<ParticipationEntity> entities);

    default Set<MemberId> toMemberIds(List<ParticipationEntity> entities) {
        if (entities == null) {
            return Set.of();
        }
        return entities.stream()
                .map(entity -> new MemberId(entity.getMember().getId()))
                .collect(Collectors.toSet());
    }
}
