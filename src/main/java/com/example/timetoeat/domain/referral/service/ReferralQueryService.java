package com.example.timetoeat.domain.referral.service;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.exception.MemberErrorCode;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.referral.dto.response.ReferralItemResponse;
import com.example.timetoeat.domain.referral.repository.ReferralCodeRepository;
import com.example.timetoeat.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReferralQueryService {

    private final ReferralCodeRepository referralRepo;
    private final MemberJpaRepository memberRepo;

    public List<ReferralItemResponse> getMyReferrals(Long meId) {
        MemberEntity me = memberRepo.findById(meId)
                .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));

        return referralRepo.findAllByInviter_IdOrderByCreatedAtDesc(me.getId())
                .stream().map(ReferralItemResponse::from).toList();
    }
}
