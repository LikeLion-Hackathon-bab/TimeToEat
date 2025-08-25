// src/main/java/com/example/timetoeat/domain/meal/service/MealStatusService.java
package com.example.timetoeat.domain.meal.service;

import com.example.timetoeat.domain.meal.dto.request.UpdateMealStatusRequest.Action;
import com.example.timetoeat.domain.meal.dto.response.MealStatusResponse;
import com.example.timetoeat.domain.meal.entity.MemberMealStatus;
import com.example.timetoeat.domain.meal.repository.MemberMealStatusRepository;
import com.example.timetoeat.domain.member.exception.MemberErrorCode;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.global.error.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

@Service
@RequiredArgsConstructor
@Transactional
public class MealStatusService {

    private static final Duration WINDOW = Duration.ofHours(4);

    private final MemberMealStatusRepository repo;
    private final MemberJpaRepository memberRepo;
    @Qualifier("kstClock")
    private final Clock clock;

    // 게시글 작성/태그에 의해 "먹음" 처리
    public void markAte(Long memberId) {
        var now = LocalDateTime.now(clock);
        var status = getOrCreate(memberId);
        status.ate(now);
    }

    // 토글 API: ATE_NOW / SET_OFF
    public void update(Long memberId, Action action) {
        var status = getOrCreate(memberId);
        var now = LocalDateTime.now(clock);
        switch (action) {
            case ATE_NOW -> status.ate(now);
            case SET_OFF -> status.setManualFasting(now);
        }
    }

    @Transactional(readOnly = true)
    public MealStatusResponse getMyStatus(Long memberId) {
        var now = LocalDateTime.now(clock);
        var sOpt = repo.findByMember_Id(memberId);
        if (sOpt.isEmpty()) {
            // 기록이 없으면 공복 상태로 간주(0시간 공복)
            return MealStatusResponse.fasting(null, 0, 0);
        }
        var s = sOpt.get();
        LocalDateTime base = s.getLastMealAt();
        // 수동 OFF가 더 최근이면 그 시점부터 공복으로 간주
        if (s.getManualFastingSince() != null
                && (base == null || s.getManualFastingSince().isAfter(base))) {
            base = s.getManualFastingSince();
            long mins = Duration.between(base, now).toMinutes();
            return MealStatusResponse.fasting(s.getLastMealAt(), mins, mins / 60);
        }

        if (base == null) {
            long mins = 0;
            return MealStatusResponse.fasting(null, mins, mins / 60);
        }

        LocalDateTime offAt = base.plus(WINDOW);
        if (now.isBefore(offAt)) {
            long secLeft = Duration.between(now, offAt).toSeconds();
            return MealStatusResponse.fed(s.getLastMealAt(), secLeft);
        } else {
            long mins = Duration.between(base, now).toMinutes();
            return MealStatusResponse.fasting(s.getLastMealAt(), mins, mins / 60);
        }
    }

    private MemberMealStatus getOrCreate(Long memberId) {
        var m = memberRepo.findById(memberId)
                .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));
        return repo.findByMember_Id(memberId).orElseGet(() -> repo.save(MemberMealStatus.of(m)));
    }

    public void markAte(Long memberId, LocalDateTime when) {
        var status = getOrCreate(memberId);
        status.ate(when); // lastMealAt = when, manualFastingSince = null
    }
}
