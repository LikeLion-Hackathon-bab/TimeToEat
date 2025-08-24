package com.example.timetoeat.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateProfileRequest {

    @NotBlank
    @Size(max = 50)
    private String userName;

    @Size(max = 512)
    private String profileImageUrl;

    @Size(max = 150)
    private String bio;  // 소개글 (nullable)
}
