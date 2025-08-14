package com.example.timetoeat.domain.article.repository;

import com.example.timetoeat.domain.article.entity.ArticleLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ArticleLikeRepository extends JpaRepository<ArticleLike, Long> {

    // '좋아요' 현재 상태 확인(엔티티 조회)
    Optional<ArticleLike> findByArticle_IdAndMember_Id(Long articleId, Long memberId);

    // 해당 게시글에 '좋아요' 눌렀는지 여부
    boolean existsByArticle_IdAndMember_Id(Long articleId, Long memberId);

    // '좋아요' 취소
    void deleteByArticle_IdAndMember_Id(Long articleId, Long memberId);

    // 특정 회원이 주어진 게시글들 중 '좋아요'한 게시글의 ID만 골라서 반환
    @Query("select al.article.id from ArticleLike al where al.member.id = :memberId and al.article.id in :articleIds")
    List<Long> findLikedArticleIdsByMember(Long memberId, Collection<Long> articleIds);
}
