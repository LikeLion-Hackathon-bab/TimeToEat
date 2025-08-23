package com.example.timetoeat.domain.member.service;


import com.example.timetoeat.domain.member.dto.request.UpdateProfileRequest;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.exception.MemberErrorCode;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberProfileCommandService {

    private final MemberJpaRepository memberJpaRepository;

    // 1) DTO 버전: 공통 필드 버전으로 위임 (검증/trim 일원화)
    public void updateProfile(Long meId, UpdateProfileRequest request) {
        updateProfile(meId, request.getUserName(), request.getProfileImageUrl(), request.getBio());
    }

    // "" -> null
    private String norm(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    // 2) 필드 버전: 유효성/trim 가드 한 곳에서 처리
    public void updateProfile(Long meId, String userName, String profileImageUrl, String bio) {
        MemberEntity me = memberJpaRepository.findById(meId)
                .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));

        String u = userName == null ? null : userName.trim();
        if (u != null && u.isEmpty()) {
            throw new CustomException(MemberErrorCode.INVALID_USERNAME);
        }
        me.updateProfile(
                u,
                norm(profileImageUrl),
                norm(bio)
        );
    }
}
