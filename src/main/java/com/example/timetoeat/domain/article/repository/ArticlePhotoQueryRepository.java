package com.example.timetoeat.domain.article.repository;

import com.example.timetoeat.domain.article.entity.Article;
import com.example.timetoeat.domain.article.repository.projection.ArticlePhotoProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import java.time.LocalDateTime;

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
}
