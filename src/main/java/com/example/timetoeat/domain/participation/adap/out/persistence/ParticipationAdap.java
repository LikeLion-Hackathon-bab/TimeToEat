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

    /**
     * 참여 명단에 새로 들어온 회원만 추가한다. 도메인(Participation.add)에는 제거가 없으므로 기존 행은 건드리지 않는다.
     * 이전 구현은 공고의 행을 모두 지운 뒤 다시 넣어, 행 id가 바뀌고 오래된 스냅샷으로 저장하면
     * 그 사이 커밋된 다른 신청자 행을 지웠다(현재 호출부는 공고 잠금 안이라 드러나지 않았던 잠재 결함).
     */
    @Override
    public void save(Participation participation) {
        if (participation.getMemberIds() == null || participation.getMemberIds().isEmpty()) return;
        Long postId = participation.getPostId().getId();
        java.util.Set<Long> existing = participationRepository.findAllByPostId(postId).stream()
                .map(e -> e.getMember().getId())
                .collect(Collectors.toSet());
        List<Long> missing = participation.getMemberIds().stream()
                .map(m -> m.getId())
                .filter(id -> !existing.contains(id))
                .distinct()
                .toList();
        if (missing.isEmpty()) return;

        PostEntity postEntity = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalStateException("Post not found while saving participation."));
        List<ParticipationEntity> newParticipations = missing.stream()
                .map(memberId -> new ParticipationEntity(postEntity, memberRepository.findById(memberId)
                        .orElseThrow(() -> new IllegalStateException("Member not found while saving participation."))))
                .collect(Collectors.toList());
        participationRepository.saveAll(newParticipations);
    }
}
