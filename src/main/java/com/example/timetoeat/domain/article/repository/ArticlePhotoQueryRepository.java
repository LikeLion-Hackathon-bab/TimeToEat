package com.example.timetoeat.domain.article.repository;

import com.example.timetoeat.domain.article.entity.Article;
import com.example.timetoeat.domain.article.repository.projection.ArticlePhotoProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ArticlePhotoQueryRepository extends JpaRepository<Article, Long> {

    // 작성자가 올린 사진
    @Query("""
               select a.id as articleId, a.imageUrl as imageUrl, a.createdAt as createdAt,
                      a.mealDate as mealDate, a.mealTime as mealTime
               from Article a
               where a.author.id = :memberId
                 and (:since is null or a.createdAt >= :since)
               order by a.createdAt desc, a.id desc
            """)
    Page<ArticlePhotoProjection> findAuthorPhotos(Long memberId, LocalDateTime since, Pageable pageable);

    // 태그된 사진
    @Query("""
               select a.id as articleId, a.imageUrl as imageUrl, a.createdAt as createdAt,
                      a.mealDate as mealDate, a.mealTime as mealTime
               from ArticleTag t
                 join t.article a
               where t.taggedMember.id = :memberId
                 and (:since is null or a.createdAt >= :since)
               order by a.createdAt desc, a.id desc
            """)
    Page<ArticlePhotoProjection> findTaggedPhotos(Long memberId, LocalDateTime since, Pageable pageable);

    @Query(nativeQuery = true, value = """
        SELECT articleId, imageUrl, createdAt, mealDate, mealTime FROM (
            SELECT a.id AS articleId, a.image_url AS imageUrl, a.created_at AS createdAt,
                   a.meal_date AS mealDate, a.meal_time AS mealTime
            FROM article a
            WHERE a.member_id = :memberId
              AND a.created_at >= :since
            UNION
            SELECT a.id AS articleId, a.image_url AS imageUrl, a.created_at AS createdAt,
                   a.meal_date AS mealDate, a.meal_time AS mealTime
            FROM article_tag t
            JOIN article a ON t.article_id = a.id
            WHERE t.tagged_member_id = :memberId
              AND a.created_at >= :since
        ) AS combined_articles
        ORDER BY createdAt DESC, articleId DESC
    """)
    List<ArticlePhotoProjection> findAllUserPhotosSince(Long memberId, LocalDateTime since);

}
