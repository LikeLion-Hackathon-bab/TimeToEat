package com.example.timetoeat.domain.article.dto.response;

import com.example.timetoeat.domain.article.repository.projection.ArticlePhotoProjection;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
public record ArticlePhotoResponse(Long articleId,
                                   String imageUrl,
                                   LocalDateTime createdAt,
                                   LocalDate mealDate,
                                   LocalTime mealTime) {

    public static ArticlePhotoResponse from(ArticlePhotoProjection p) {
        return ArticlePhotoResponse.builder()
                .articleId(p.getArticleId())
                .imageUrl(p.getImageUrl())
                .createdAt(p.getCreatedAt())
                .mealDate(p.getMealDate())
                .mealTime(p.getMealTime())
                .build();
    }
}
