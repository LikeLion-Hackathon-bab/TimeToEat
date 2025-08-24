package com.example.timetoeat.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileOnboardingRequest {

    @NotBlank
    @Size(max = 50)
    private String userName;

    @Size(max = 512)
    private String profileImageUrl;

    @Size(max = 150)
    private String bio;

    // 8자리 숫자 코드만 허용
    private static final String CODE_REGEX = "^[0-9]{8}$";
    public static final String CODE_MSG   = "코드는 8자리 숫자여야 합니다.";

    @Size(max = 200)
    private List<@Pattern(regexp = CODE_REGEX, message = CODE_MSG) String> likedCodes;

    @Size(max = 200)
    private List<@Pattern(regexp = CODE_REGEX, message = CODE_MSG) String> dislikedCodes;

    @Size(max = 200)
    private List<@Pattern(regexp = CODE_REGEX, message = CODE_MSG) String> allergyCodes;
}
