package com.example.timetoeat.domain.article.dto.response;

import com.example.timetoeat.domain.article.entity.Article;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleSummaryResponse {

    private Long articleId;
    private Long authorId;

    private String imageUrl;

    private LocalDate mealDate;
    private LocalTime mealTime;

    private String restaurantName;

    private int likeCount;
    private int commentCount;

    private boolean likedByMe;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    private List<Long> taggedMemberIds;

    @Builder
    private ArticleSummaryResponse(Long articleId, Long authorId, String imageUrl,
                                   LocalDate mealDate, LocalTime mealTime, String restaurantName,
                                   int likeCount, int commentCount, boolean likedByMe,
                                   LocalDateTime createdAt, LocalDateTime expiresAt,
                                   List<Long> taggedMemberIds) {

        this.articleId = articleId;
        this.authorId = authorId;
        this.imageUrl = imageUrl;
        this.mealDate = mealDate;
        this.mealTime = mealTime;
        this.restaurantName = restaurantName;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.likedByMe = likedByMe;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.taggedMemberIds = taggedMemberIds;
    }

    public static ArticleSummaryResponse from(Article article, boolean likedByMe, List<Long> taggedMemberIds) {
        return ArticleSummaryResponse.builder()
                .articleId(article.getId())
                .authorId(article.getAuthor().getId())
                .imageUrl(article.getImageUrl())
                .mealDate(article.getMealDate())
                .mealTime(article.getMealTime())
                .restaurantName(article.getRestaurant() != null ? article.getRestaurant().getPlaceName() : null)
                .likeCount(article.getLikeCount())
                .commentCount(article.getCommentCount())
                .likedByMe(likedByMe)
                .createdAt(article.getCreatedAt())
                .expiresAt(article.getExpiresAt())
                .taggedMemberIds(taggedMemberIds)
                .build();
    }

}
