package com.example.timetoeat.domain.article.repository;

import com.example.timetoeat.domain.article.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    // 홈 피드: 생성 24h 이내만 노출 + 최신순
    Page<Article> findByCreatedAtGreaterThanEqual(LocalDateTime threshold, Pageable pageable);

    // 프로필(내 밥그릇 포함): 특정 작성자의 전체 글 + 최신순
    Page<Article> findByAuthor_Id(Long authorId, Pageable pageable);

    // 삭제 권한 확인 (글 작성자인지)
    boolean existsByIdAndAuthor_Id(Long articleId, Long authorId);
}
