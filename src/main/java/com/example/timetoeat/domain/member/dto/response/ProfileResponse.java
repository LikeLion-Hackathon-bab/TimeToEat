package com.example.timetoeat.domain.member.dto.response;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileResponse {

    private Long memberId;
    private String userName;
    private String profileImageUrl;
    private String bio;
    private int meetingCount;

    @Builder
    private ProfileResponse(Long memberId, String userName, String profileImageUrl,
                             String bio, int meetingCount) {

        this.memberId = memberId;
        this.userName = userName;
        this.profileImageUrl = profileImageUrl;
        this.bio = bio;
        this.meetingCount = meetingCount;
    }

    public static ProfileResponse from(MemberEntity e) {
        return ProfileResponse.builder()
                .memberId(e.getId())
                .userName(e.getUsername())
                .profileImageUrl(e.getProfileImageUrl())
                .bio(e.getBio())
                .meetingCount(e.getMeetingCount())
                .build();
    }
}
