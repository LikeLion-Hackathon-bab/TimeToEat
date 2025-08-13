package com.example.timetoeat.domain.article.dto.response;

import com.example.timetoeat.domain.article.entity.Article;
import com.example.timetoeat.domain.article.entity.RestaurantSnapshot;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleDetailResponse {

    private Long articleId;
    private Long authorId;

    private String imageUrl;

    private LocalDate mealDate;
    private LocalTime mealTime;

    private RestaurantInfo restaurant;

    private int likeCount;
    private int commentCount;

    private boolean likedByMe;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    @Builder
    private ArticleDetailResponse(Long articleId, Long authorId, String imageUrl,
                                  LocalDate mealDate, LocalTime mealTime, RestaurantInfo restaurant,
                                  int likeCount, int commentCount, boolean likedByMe,
                                  LocalDateTime createdAt, LocalDateTime expiresAt) {

        this.articleId = articleId;
        this.authorId = authorId;
        this.imageUrl = imageUrl;
        this.mealDate = mealDate;
        this.mealTime = mealTime;
        this.restaurant = restaurant;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.likedByMe = likedByMe;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static ArticleDetailResponse from(Article article, boolean likedByMe) {
        return ArticleDetailResponse.builder()
                .articleId(article.getId())
                .authorId(article.getAuthor().getId())
                .imageUrl(article.getImageUrl())
                .mealDate(article.getMealDate())
                .mealTime(article.getMealTime())
                .restaurant(RestaurantInfo.from(article.getRestaurant()))
                .likeCount(article.getLikeCount())
                .commentCount(article.getCommentCount())
                .likedByMe(likedByMe)
                .createdAt(article.getCreatedAt())
                .expiresAt(article.getExpiresAt())
                .build();
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class RestaurantInfo {
        private String placeId;
        private String placeName;
        private String addressName;
        private String roadAddressName;
        private String phoneNumber;
        private String placeUrl;

        private String categoryGroupCode;
        private String categoryGroupName;
        private String categoryName;

        private Double x;
        private Double y;

        @Builder
        private RestaurantInfo(String placeId, String placeName, String addressName, String roadAddressName,
                               String phoneNumber, String placeUrl, String categoryGroupCode,
                               String categoryGroupName, String categoryName, Double x, Double y) {

            this.placeId = placeId;
            this.placeName = placeName;
            this.addressName = addressName;
            this.roadAddressName = roadAddressName;
            this.phoneNumber = phoneNumber;
            this.placeUrl = placeUrl;
            this.categoryGroupCode = categoryGroupCode;
            this.categoryGroupName = categoryGroupName;
            this.categoryName = categoryName;
            this.x = x;
            this.y = y;
        }

        public static RestaurantInfo from(RestaurantSnapshot restaurant) {
            if (restaurant == null) {
                return null;
            }

            return RestaurantInfo.builder()
                    .placeId(restaurant.getPlaceId())
                    .placeName(restaurant.getPlaceName())
                    .addressName(restaurant.getAddressName())
                    .roadAddressName(restaurant.getRoadAddressName())
                    .phoneNumber(restaurant.getPhoneNumber())
                    .placeUrl(restaurant.getPlaceUrl())
                    .categoryGroupCode(restaurant.getCategoryGroupCode())
                    .categoryGroupName(restaurant.getCategoryGroupName())
                    .categoryName(restaurant.getCategoryName())
                    .x(restaurant.getX())
                    .y(restaurant.getY())
                    .build();

        }
    }
}
