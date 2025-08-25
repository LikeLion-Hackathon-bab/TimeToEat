package com.example.timetoeat.domain.referral.dto.response;

import com.example.timetoeat.domain.referral.entity.ReferralCode;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReferralItemResponse {

    private String code;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private boolean used;
    private Long usedByMemberId;
    private String usedByUserName;

    @Builder
    private ReferralItemResponse(String code, LocalDateTime createdAt, LocalDateTime expiresAt,
                                 boolean used, Long usedByMemberId, String usedByUserName) {
        this.code = code;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.used = used;
        this.usedByMemberId = usedByMemberId;
        this.usedByUserName = usedByUserName;
    }

    public static ReferralItemResponse from(ReferralCode e) {
        return ReferralItemResponse.builder()
                .code(e.getCode())
                .createdAt(e.getCreatedAt())
                .expiresAt(e.getExpiresAt())
                .used(e.isUsed())
                .usedByMemberId(e.isUsed() ? e.getUsedBy().getId() : null)
                .usedByUserName(e.isUsed() ? e.getUsedBy().getUsername() : null)
                .build();
    }
}
