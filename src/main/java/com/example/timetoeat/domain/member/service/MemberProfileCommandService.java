package com.example.timetoeat.domain.member.service;


import com.example.timetoeat.domain.member.dto.request.UpdateProfileRequest;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.global.error.GlobalErrorCode;
import com.example.timetoeat.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberProfileCommandService {

    private final MemberJpaRepository memberJpaRepository;

    public void updateProfile(Long meId, UpdateProfileRequest request) {
        MemberEntity me = memberJpaRepository.findById(meId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND, "멤버를 찾을 수 없습니다."));

        me.updateProfile(request.getUserName(), request.getProfileImageUrl(), request.getBio());
    }
}
