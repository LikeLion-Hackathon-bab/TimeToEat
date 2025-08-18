package com.example.timetoeat.domain.article.entity;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.global.util.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;


@Getter
@Entity
@Table(name = "article_comment",
        indexes = {
                @Index(name = "idx_comment_article", columnList = "article_id"),
                @Index(name = "idx_comment_parent",  columnList = "parent_id")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleComment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private MemberEntity author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private ArticleComment parentComment;  // 대댓글

    @Column(nullable = false, length = 500)
    private String content;

    @Builder
    private ArticleComment(MemberEntity author, Article article, ArticleComment parentComment, String content) {
        this.author = Objects.requireNonNull(author, "author must not be null");
        this.article = Objects.requireNonNull(article, "article must not be null");
        this.parentComment = parentComment;
        this.content = Objects.requireNonNull(content, "content must not be null");
    }

    // 본인 댓글인지
    public boolean isWrittenBy(Long memberId) {
        return author != null && author.getId().equals(memberId);
    }
}
