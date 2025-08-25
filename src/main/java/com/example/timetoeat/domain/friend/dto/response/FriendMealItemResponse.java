// src/main/java/com/example/timetoeat/domain/friend/dto/response/FriendMealItemResponse.java
package com.example.timetoeat.domain.friend.dto.response;

import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendMealItemResponse {

    private Long memberId;
    private String userName;
    private String profileImageUrl;
    private boolean hungry;  // true=배고파(OFF)
    private String label;    // "4시간 공복이에요" / "방금 먹었어요"

    @Builder
    private FriendMealItemResponse(Long memberId, String userName, String profileImageUrl,
                                   boolean hungry, String label) {
        this.memberId = memberId;
        this.userName = userName;
        this.profileImageUrl = profileImageUrl;
        this.hungry = hungry;
        this.label = label;
    }

    public static FriendMealItemResponse of(Long id, String name, String img, boolean hungry, String label) {

        return FriendMealItemResponse.builder()
                .memberId(id).userName(name).profileImageUrl(img)
                .hungry(hungry).label(label).build();
    }
}
