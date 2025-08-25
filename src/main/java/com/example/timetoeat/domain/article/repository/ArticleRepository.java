package com.example.timetoeat.domain.article.repository;

import com.example.timetoeat.domain.article.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    // 홈 피드: 생성 24h 이내만 노출 + 최신순
    @EntityGraph(attributePaths = "author")
    Page<Article> findByCreatedAtGreaterThanEqual(LocalDateTime threshold, Pageable pageable);

    // 프로필(내 밥그릇 포함): 특정 작성자의 전체 글 + 최신순
    @EntityGraph(attributePaths = "author")
    Page<Article> findByAuthor_Id(Long authorId, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Optional<Article> findById(Long id);

    // 삭제 권한 확인 (글 작성자인지)
    boolean existsByIdAndAuthor_Id(Long articleId, Long authorId);

    // 글 ID와 작성자 ID가 모두 일치할 때만 삭제 (삭제 성공 시 1, 없으면 0 반환)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Article a where a.id = :articleId and a.author.id = :authorId")
    int deleteByIdAndAuthorId(@Param("articleId")Long articleId, @Param("authorId") Long authorId);

    // [start, end): 시작 포함, 끝 미포함
    long countByAuthor_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long authorId,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );
}
