package com.example.timetoeat.domain.article.repository;

import com.example.timetoeat.domain.article.entity.MealLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MealLogRepository extends JpaRepository<MealLog, Long> {

    Optional<MealLog> findByArticle_Id(Long articleId);

    @Query("select distinct m.code from MealLog m where m.member.id = :memberId and m.tsUtc >= :since")
    List<String> findCodesSince(Long memberId, Instant since);
}
