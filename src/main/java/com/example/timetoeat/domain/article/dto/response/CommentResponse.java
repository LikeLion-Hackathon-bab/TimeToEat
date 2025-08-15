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
    private String authorUsername;
    private Long parentCommentId;
    private String content;
    private LocalDateTime createdAt;

    @Builder
    CommentResponse(Long commentId, Long authorId, String authorUsername,
                    Long parentCommentId, String content, LocalDateTime createdAt) {

        this.commentId = commentId;
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.parentCommentId = parentCommentId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static CommentResponse from(ArticleComment comment) {
        return CommentResponse.builder()
                .commentId(comment.getId())
                .authorId(comment.getAuthor().getId())
                .authorUsername(comment.getAuthor().getUsername())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
