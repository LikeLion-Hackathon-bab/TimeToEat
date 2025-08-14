package com.example.timetoeat.domain.article.repository;

import com.example.timetoeat.domain.article.entity.ArticleTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ArticleTagRepository extends JpaRepository<ArticleTag, Long> {

    // 특정 게시글 ArticleTag 엔티티 전체 조회
    List<ArticleTag> findByArticle_Id(Long articleId);

    // 특정 게시글의 태그된 멤버 ID만 조회
    @Query("select t.taggedMember.id from ArticleTag t where t.article.id = :articleId")
    List<Long> findTaggedMemberIdsByArticleId(Long articleId);

    // 특정 게시글의 모든 태그를 한 번에 삭제
    void deleteByArticle_Id(Long articleId);

    // 여러 게시글의 태그를 (articleId, taggedMemberId) 쌍 목록으로 조회
    @Query("select t.article.id, t.taggedMember.id from ArticleTag t where t.article.id in :articleIds")
    List<Object[]> findTaggedMemberIdsByArticleIds(Collection<Long> articleIds);
}
