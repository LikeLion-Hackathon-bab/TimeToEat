package com.example.timetoeat.domain.article.dto.response;

import com.example.timetoeat.domain.article.entity.Article;
import com.example.timetoeat.domain.article.entity.UploadMethod;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleSummaryResponse {

    private Long articleId;
    private Long authorId;
    private String authorUsername;

    private String imageUrl;

    private LocalDate mealDate;
    private LocalTime mealTime;

    private String restaurantName;

    // 피드 식당 카드(시안 card_restaurant_pic)에 주소·분류·좌표가 필요하다. restaurantName 은 하위 호환으로 남긴다.
    private ArticleDetailResponse.RestaurantInfo restaurant;

    private int likeCount;
    private int commentCount;

    private boolean likedByMe;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    private List<Long> taggedMemberIds;

    // 사진을 올린 방법(CAMERA/ALBUM). 이 값이 저장되기 전 글은 null.
    private UploadMethod method;

    @Builder
    private ArticleSummaryResponse(Long articleId, Long authorId, String authorUsername, String imageUrl,
                                   LocalDate mealDate, LocalTime mealTime, String restaurantName,
                                   ArticleDetailResponse.RestaurantInfo restaurant,
                                   int likeCount, int commentCount, boolean likedByMe,
                                   LocalDateTime createdAt, LocalDateTime expiresAt,
                                   List<Long> taggedMemberIds, UploadMethod method) {

        this.articleId = articleId;
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.imageUrl = imageUrl;
        this.mealDate = mealDate;
        this.mealTime = mealTime;
        this.restaurantName = restaurantName;
        this.restaurant = restaurant;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.likedByMe = likedByMe;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.taggedMemberIds = taggedMemberIds;
        this.method = method;
    }

    public static ArticleSummaryResponse from(Article article, boolean likedByMe, List<Long> taggedMemberIds) {

        return ArticleSummaryResponse.builder()
                .articleId(article.getId())
                .authorId(article.getAuthor().getId())
                .authorUsername(article.getAuthor().getUsername())
                .imageUrl(article.getImageUrl())
                .mealDate(article.getMealDate())
                .mealTime(article.getMealTime())
                .restaurantName(article.getRestaurant() != null ? article.getRestaurant().getPlaceName() : null)
                .restaurant(ArticleDetailResponse.RestaurantInfo.from(article.getRestaurant()))
                .likeCount(article.getLikeCount())
                .commentCount(article.getCommentCount())
                .likedByMe(likedByMe)
                .createdAt(article.getCreatedAt())
                .expiresAt(article.getExpiresAt())
                .taggedMemberIds(taggedMemberIds)
                .method(article.getMethod())
                .build();
    }

}
