package com.example.timetoeat.domain.member.service;


import com.example.timetoeat.domain.member.dto.response.ProfileResponse;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.global.error.GlobalErrorCode;
import com.example.timetoeat.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberProfileQueryService {

    private final MemberJpaRepository memberJpaRepository;

    public ProfileResponse getMyProfile(Long meId) {
        return getProfile(meId);
    }

    public ProfileResponse getProfile(Long memberId) {
        MemberEntity target = memberJpaRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND, "멤버를 찾을 수 없습니다."));

        return ProfileResponse.from(target);
    }
}
