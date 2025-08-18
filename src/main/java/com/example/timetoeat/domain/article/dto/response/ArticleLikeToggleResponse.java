package com.example.timetoeat.domain.article.dto.response;


import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleLikeToggleResponse {

    private boolean liked;  // 좋아요 토글 현재 상태
    private int likeCount;

    @Builder
    private ArticleLikeToggleResponse(boolean liked, int likeCount) {
        this.liked = liked;
        this.likeCount = likeCount;
    }

    public static ArticleLikeToggleResponse of(boolean liked, int likeCount) {
        return ArticleLikeToggleResponse.builder()
                .liked(liked)
                .likeCount(likeCount)
                .build();
    }
}