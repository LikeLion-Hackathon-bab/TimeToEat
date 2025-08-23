package com.example.timetoeat.domain.article.service;

import com.example.timetoeat.domain.article.dto.response.RecentMealsResponse;
import com.example.timetoeat.domain.article.entity.MealLog;
import com.example.timetoeat.domain.article.repository.MealLogRepository;
import com.example.timetoeat.domain.preference.exception.PreferenceErrorCode;
import com.example.timetoeat.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleMealQueryService {

    private final MealLogRepository mealLogRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public RecentMealsResponse getMyRecentMeals(Long memberId) {

        if (memberId == null) {
            throw new CustomException(PreferenceErrorCode.UNAUTHENTICATED);
        }

        Instant to = Instant.now(clock);
        Instant from = to.minus(72, ChronoUnit.HOURS);

        List<MealLog> logs = mealLogRepository.findAllByMember_IdAndTsUtcBetweenOrderByTsUtcDesc(memberId, from, to);
        return RecentMealsResponse.from(memberId, logs);
    }
}
