package com.example.timetoeat.domain.participation.application.service;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.participation.adap.in.dto.ParticipationSocketDto;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ParticipationInfoService {
    private final MemberJpaRepository memberJpaRepository;

    public List<ParticipationSocketDto> getParticipations(Set<MemberId> memberIds) {
        return memberIds.stream()
                .map(memberId -> {
                    MemberEntity memberEntity = memberJpaRepository.findById(memberId.getId())
                            .orElseThrow(() -> new IllegalStateException("회원을 찾을 수 없습니다."));
                    return new ParticipationSocketDto(
                            String.valueOf(memberEntity.getId()),
                            memberEntity.getUsername(),
                            memberEntity.getProfileImageUrl()
                    );
                })
                .toList();
    }
}
