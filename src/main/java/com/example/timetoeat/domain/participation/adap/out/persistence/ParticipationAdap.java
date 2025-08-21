package com.example.timetoeat.domain.participation.adap.out.persistence;

import com.example.timetoeat.domain.participation.application.port.out.LoadParticipation;
import com.example.timetoeat.domain.participation.application.port.out.SaveParticipation;
import com.example.timetoeat.domain.participation.adap.out.mapper.ParticipationMapper;
import com.example.timetoeat.domain.post.adap.out.persistence.PostEntity;
import com.example.timetoeat.domain.post.adap.out.persistence.PostRepository;
import com.example.timetoeat.domain.participation.domain.Participation;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ParticipationAdap implements LoadParticipation, SaveParticipation {

    private final ParticipationRepository participationRepository;
    private final ParticipationMapper participationMapper;
    private final PostRepository postRepository;
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
