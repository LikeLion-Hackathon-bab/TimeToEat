package com.example.timetoeat.domain.referral.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReferralCreateResponse {

    private String code;
    private String link;
    private LocalDateTime expiresAt;

    @Builder
    private ReferralCreateResponse(String code, String link, LocalDateTime expiresAt) {
        this.code = code;
        this.link = link;
        this.expiresAt = expiresAt;
    }

    public static ReferralCreateResponse of(String code, String link, LocalDateTime expiresAt) {
        return ReferralCreateResponse.builder().code(code).link(link).expiresAt(expiresAt).build();
    }
}
