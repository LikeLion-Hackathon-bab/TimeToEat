package com.example.timetoeat.domain.article.repository;

import com.example.timetoeat.domain.article.entity.ArticleLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleLikeRepository extends JpaRepository<ArticleLike, Long> {

    // '좋아요' 현재 상태 확인(엔티티 조회)
    Optional<ArticleLike> findByArticle_IdAndMember_Id(Long articleId, Long memberId);

    // 해당 게시글에 '좋아요' 눌렀는지 여부
    boolean existsByArticle_IdAndMember_Id(Long articleId, Long memberId);

    // '좋아요' 취소
    void deleteByArticle_IdAndMember_Id(Long articleId, Long memberId);
}
