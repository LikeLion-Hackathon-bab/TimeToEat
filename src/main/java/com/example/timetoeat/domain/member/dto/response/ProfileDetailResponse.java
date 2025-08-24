package com.example.timetoeat.domain.member.dto.response;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileDetailResponse {

    private Long memberId;
    private String userName;
    private String profileImageUrl;
    private String bio;
    private int meetingCount;

    private List<PreferenceItem> likes;
    private List<PreferenceItem> dislikes;
    private List<PreferenceItem> allergies;

    @Builder
    private ProfileDetailResponse(Long memberId, String userName, String profileImageUrl,
                                  String bio, int meetingCount,
                                  List<PreferenceItem> likes,
                                  List<PreferenceItem> dislikes,
                                  List<PreferenceItem> allergies) {

        this.memberId = memberId;
        this.userName = userName;
        this.profileImageUrl = profileImageUrl;
        this.bio = bio;
        this.meetingCount = meetingCount;
        this.likes = likes;
        this.dislikes = dislikes;
        this.allergies = allergies;
    }

    public static ProfileDetailResponse of(MemberEntity m,
                                           List<PreferenceItem> likes,
                                           List<PreferenceItem> dislikes,
                                           List<PreferenceItem> allergies) {
        return ProfileDetailResponse.builder()
                .memberId(m.getId())
                .userName(m.getUsername())
                .profileImageUrl(m.getProfileImageUrl())
                .bio(m.getBio())
                .meetingCount(m.getMeetingCount())
                .likes(likes)
                .dislikes(dislikes)
                .allergies(allergies)
                .build();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class PreferenceItem {
        private String code;
        private String label;
    }
}
