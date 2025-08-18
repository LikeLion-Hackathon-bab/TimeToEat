package com.example.timetoeat.domain.article.entity;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.global.util.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.util.Objects;

@Getter
@Entity
@Table(name = "meal_log",
        uniqueConstraints = @UniqueConstraint(name = "uk_meal_log_article", columnNames = "article_id"),
        indexes = {
                @Index(name = "idx_meal_log_member_ts", columnList = "member_id, ts_utc"),
                @Index(name = "idx_meal_log_code", columnList = "code")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Column(nullable = false, length = 16)
    private String code;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false)
    private double confidence;

    // 섭취 시각(UTC)
    @Column(name = "ts_utc", nullable = false)
    private Instant tsUtc;

    @Builder
    private MealLog(Article article, MemberEntity member, String code, String label, double confidence, Instant tsUtc) {
        this.article = Objects.requireNonNull(article);
        this.member  = Objects.requireNonNull(member);
        this.code = Objects.requireNonNull(code);
        this.label = Objects.requireNonNull(label);
        this.confidence = confidence;
        this.tsUtc = Objects.requireNonNull(tsUtc);
    }

    public void update(String code, String label, double confidence, Instant tsUtc) {
        this.code = code;
        this.label = label;
        this.confidence = confidence;
        this.tsUtc = tsUtc;
    }
}
