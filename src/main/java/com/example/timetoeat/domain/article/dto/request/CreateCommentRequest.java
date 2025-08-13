package com.example.timetoeat.domain.article.dto.request;

import com.example.timetoeat.domain.article.entity.Article;
import com.example.timetoeat.domain.article.entity.ArticleComment;
import com.example.timetoeat.global.auth.entity.MemberEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreateCommentRequest {

    private Long parentCommentId;

    @NotBlank
    @Size(max = 500)
    private String content;

    @Builder
    private CreateCommentRequest(Long parentCommentId, String content) {
        this.parentCommentId = parentCommentId;
        this.content = content;
    }

    public ArticleComment toEntity(MemberEntity author, Article article, ArticleComment parentComment) {
        return ArticleComment.builder()
                .author(author)
                .article(article)
                .parentComment(parentComment)
                .content(content)
                .build();
    }
}
