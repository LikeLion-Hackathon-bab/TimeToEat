package com.example.timetoeat.domain.article.repository.projection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public interface ArticlePhotoProjection {

    Long getArticleId();
    String getImageUrl();
    LocalDateTime getCreatedAt();
    LocalDate getMealDate();
    LocalTime getMealTime();
}
