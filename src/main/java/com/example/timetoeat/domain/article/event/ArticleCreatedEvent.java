package com.example.timetoeat.domain.article.event;

import lombok.Value;
import java.time.LocalDateTime;

@Value
public class ArticleCreatedEvent {

    Long articleId;
    Long authorId;
    String imageUrl;
    LocalDateTime mealAtKst;
}
