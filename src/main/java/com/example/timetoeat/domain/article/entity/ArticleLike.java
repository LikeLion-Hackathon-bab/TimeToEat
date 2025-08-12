package com.example.timetoeat.domain.article.entity;

import com.example.timetoeat.global.auth.entity.MemberEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@Entity
@Table(name = "article_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_article_like_article_member",
                columnNames = {"article_id", "member_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 좋아요 누른 사람
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Article article;

    @Builder
    private ArticleLike(MemberEntity member, Article article) {
        this.member = Objects.requireNonNull(member, "member must not be null");
        this.article = Objects.requireNonNull(article, "article must not be null");
    }
}
