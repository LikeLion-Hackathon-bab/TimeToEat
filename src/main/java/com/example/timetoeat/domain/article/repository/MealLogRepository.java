package com.example.timetoeat.domain.article.repository;

import com.example.timetoeat.domain.article.entity.MealLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MealLogRepository extends JpaRepository<MealLog, Long> {

    Optional<MealLog> findByArticle_IdAndMember_Id(Long articleId, Long memberId);

    @Query("select distinct m.code from MealLog m where m.member.id = :memberId and m.tsUtc >= :since")
    List<String> findCodesSince(@Param("memberId") Long memberId, @Param("since") Instant since);

    List<MealLog> findAllByMember_IdAndTsUtcBetweenOrderByTsUtcDesc(
            Long memberId, Instant from, Instant to
    );
}
