package com.example.timetoeat.domain.preference.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PreferenceMetaResponse {
    private LocalDateTime onboardedAt;
    private LocalDateTime lastUpdatedAt;
    private long revision;

    @Builder
    private PreferenceMetaResponse(LocalDateTime onboardedAt, LocalDateTime lastUpdatedAt, long revision) {
        this.onboardedAt = onboardedAt;
        this.lastUpdatedAt = lastUpdatedAt;
        this.revision = revision;
    }

    public static PreferenceMetaResponse of(LocalDateTime onboardedAt, LocalDateTime lastUpdatedAt, long revision) {
        return PreferenceMetaResponse.builder()
                .onboardedAt(onboardedAt)
                .lastUpdatedAt(lastUpdatedAt)
                .revision(revision)
                .build();
    }
}
