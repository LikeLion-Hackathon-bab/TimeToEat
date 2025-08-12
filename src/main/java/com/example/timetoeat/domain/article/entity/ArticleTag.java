package com.example.timetoeat.domain.article.entity;

import com.example.timetoeat.global.auth.entity.MemberEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "article_tag",
        uniqueConstraints = @UniqueConstraint(name = "uk_article_tag_article_member",
                columnNames = {"article_id", "tagged_member_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tagged_member_id", nullable = false)
    private MemberEntity taggedMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Article article;

    @Builder
    private ArticleTag(MemberEntity taggedMember, Article article) {
        this.taggedMember = java.util.Objects.requireNonNull(taggedMember, "taggedMember must not be null");
        this.article      = java.util.Objects.requireNonNull(article, "article must not be null");
    }

    /** 편의 팩토리 (선택) */
    public static ArticleTag of(MemberEntity taggedMember, Article article) {
        return ArticleTag.builder().taggedMember(taggedMember).article(article).build();
    }
}
