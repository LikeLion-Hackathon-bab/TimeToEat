package com.example.timetoeat.domain.article.entity;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.global.util.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Getter
@Entity
@Table(
        name = "article",
        indexes = {
                @Index(name = "idx_article_created_at", columnList = "created_at"),
                @Index(name = "idx_article_author_created_at", columnList = "author_id, created_at")
        }
)@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Article extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private MemberEntity author;

    @Column(nullable = false, length = 1024)
    private String imageUrl;

    @Column(nullable = false)
    private LocalDate mealDate;  // 날짜

    @Column(nullable = false)
    private LocalTime mealTime;  // 시간

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(
                    name = "placeUrl",
                    column = @Column(name = "restaurant_place_url", length = 1024)
            )
    })
    private RestaurantSnapshot restaurant;

    @Column(nullable = false)
    private int likeCount = 0;

    @Column(nullable = false)
    private int commentCount = 0;

    @Version
    private Long version;

    @Builder
    private Article(MemberEntity author, String imageUrl, LocalDate mealDate, LocalTime mealTime, RestaurantSnapshot restaurant) {
        this.author = Objects.requireNonNull(author, "author must not be null");
        this.imageUrl = Objects.requireNonNull(imageUrl, "imageUrl must not be null");
        this.mealDate = Objects.requireNonNull(mealDate, "mealDate must not be null");
        this.mealTime = Objects.requireNonNull(mealTime, "mealTime must not be null");
        this.restaurant = restaurant; // 선택값
    }

    public void increaseLike()    { this.likeCount++; }
    public void decreaseLike()    { this.likeCount = Math.max(0, this.likeCount - 1); }
    public void increaseComment() { this.commentCount++; }
    public void decreaseComment() { this.commentCount = Math.max(0, this.commentCount - 1); }

    // 권한 체크
    public boolean isAuthoredBy(Long memberId) {
        return author != null && Objects.equals(author.getId(), memberId);
    }

    // 게시글 작성 24시간 후 만료 (홈에서만 숨김)
    @Transient
    public LocalDateTime getExpiresAt() {
        return getCreatedAt().plus(Duration.ofHours(24));
    }
    // now(시각)와 비교하여, 홈 피드 만료 여부 확인
    @Transient
    public boolean isExpiredForHome(LocalDateTime now) {
        return getExpiresAt().isBefore(now);
    }

    public void decreaseCommentBy(int n) {
        if (n <= 0) return;
        this.commentCount = Math.max(0, this.commentCount - n);
    }
}
