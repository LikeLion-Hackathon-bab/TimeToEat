package com.example.timetoeat.domain.posting.adapter.out.persistence.participation;

import com.example.timetoeat.domain.posting.adapter.out.mapper.ParticipationMapper;
import com.example.timetoeat.domain.posting.adapter.out.persistence.post.PostEntity;
import com.example.timetoeat.domain.posting.adapter.out.persistence.post.PostJpaRepository;
import com.example.timetoeat.domain.posting.application.port.out.Query.GetParticipationQuery;
import com.example.timetoeat.domain.posting.application.port.out.save.SaveParticipationPort;
import com.example.timetoeat.domain.posting.domain.model.Participation;
import com.example.timetoeat.domain.posting.domain.vo.PostId;
import com.example.timetoeat.global.auth.entity.MemberEntity;
import com.example.timetoeat.global.auth.repository.MemberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ParticipationPersistenceAdapter implements GetParticipationQuery, SaveParticipationPort {

    private final ParticipationJpaRepository participationRepository;
    private final ParticipationMapper participationMapper;
    private final PostJpaRepository postRepository;
    private final MemberJpaRepository memberRepository;

    @Override
    public Participation getParticipationByPostId(PostId postId) {
        List<ParticipationEntity> participationEntities = participationRepository.findAllByPostId(postId.getId());
        return participationMapper.toDomain(postId, participationEntities);
    }

    @Override
    public void save(Participation participation) {
        participationRepository.deleteAllByPostId(participation.getPostId().getId());

        if (participation.getMemberIds() != null && !participation.getMemberIds().isEmpty()) {
            PostEntity postEntity = postRepository.findById(participation.getPostId().getId())
                    .orElseThrow(() -> new IllegalStateException("Post not found while saving participation."));

            List<ParticipationEntity> newParticipations = participation.getMemberIds().stream()
                    .map(memberId -> {
                        MemberEntity memberEntity = memberRepository.findById(memberId.getId())
                                .orElseThrow(() -> new IllegalStateException("Member not found while saving participation."));
                        return new ParticipationEntity(postEntity, memberEntity);
                    })
                    .collect(Collectors.toList());

            participationRepository.saveAll(newParticipations);
        }
    }
}
