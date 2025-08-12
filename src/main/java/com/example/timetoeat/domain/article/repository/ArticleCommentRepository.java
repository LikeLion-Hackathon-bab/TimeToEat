package com.example.timetoeat.domain.article.repository;

import com.example.timetoeat.domain.article.entity.ArticleComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleCommentRepository extends JpaRepository<ArticleComment, Long> {

    // 댓글 (대댓글 X) 오름차순
    List<ArticleComment> findByArticle_IdAndParentCommentIsNullOrderByIdAsc(Long articleId);

    // 특정 댓글의 대댓글 오름차순
    List<ArticleComment> findByParentComment_IdOrderByIdAsc(Long parentCommentId);

    // 본인 댓글인지 권한 확인
    boolean existsByIdAndAuthor_Id(Long commentId, Long authorId);
}
