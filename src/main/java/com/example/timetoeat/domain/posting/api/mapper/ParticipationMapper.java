package com.example.timetoeat.domain.posting.api.mapper;

import com.example.timetoeat.domain.posting.api.infra.participation.ParticipationEntity;
import com.example.timetoeat.domain.posting.domain.model.participation.Participation;
import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import com.example.timetoeat.domain.posting.domain.vo.PostId;
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
