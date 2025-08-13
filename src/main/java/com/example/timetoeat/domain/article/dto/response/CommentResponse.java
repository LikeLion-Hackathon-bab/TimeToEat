package com.example.timetoeat.domain.article.dto.response;

import com.example.timetoeat.domain.article.entity.ArticleComment;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentResponse {

    private Long commentId;
    private Long authorId;
    private Long parentCommentId;
    private String content;
    private LocalDateTime createdAt;

    @Builder
    CommentResponse(Long commentId, Long authorId, Long parentCommentId,
                    String content, LocalDateTime createdAt) {

        this.commentId = commentId;
        this.authorId = authorId;
        this.parentCommentId = parentCommentId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static CommentResponse from(ArticleComment comment) {
        return CommentResponse.builder()
                .commentId(comment.getId())
                .authorId(comment.getAuthor().getId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
