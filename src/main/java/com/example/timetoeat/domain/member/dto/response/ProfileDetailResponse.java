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
    /** 직접 입력한 알레르기 라벨(F07). 코드가 없어 allergies와 따로 준다. 입력 순서. */
    private List<String> customAllergyLabels;

    @Builder
    private ProfileDetailResponse(Long memberId, String userName, String profileImageUrl,
                                  String bio, int meetingCount,
                                  List<PreferenceItem> likes,
                                  List<PreferenceItem> dislikes,
                                  List<PreferenceItem> allergies,
                                  List<String> customAllergyLabels) {

        this.memberId = memberId;
        this.userName = userName;
        this.profileImageUrl = profileImageUrl;
        this.bio = bio;
        this.meetingCount = meetingCount;
        this.likes = likes;
        this.dislikes = dislikes;
        this.allergies = allergies;
        this.customAllergyLabels = customAllergyLabels == null ? List.of() : customAllergyLabels;
    }

    public static ProfileDetailResponse of(MemberEntity m,
                                           List<PreferenceItem> likes,
                                           List<PreferenceItem> dislikes,
                                           List<PreferenceItem> allergies,
                                           List<String> customAllergyLabels) {
        return ProfileDetailResponse.builder()
                .memberId(m.getId())
                .userName(m.getUsername())
                .profileImageUrl(m.getProfileImageUrl())
                .bio(m.getBio())
                .meetingCount(m.getMeetingCount())
                .likes(likes)
                .dislikes(dislikes)
                .allergies(allergies)
                .customAllergyLabels(customAllergyLabels)
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
