package com.example.timetoeat.qa;

import com.example.timetoeat.domain.preference.dto.request.OnboardingPreferenceRequest;
import com.example.timetoeat.domain.preference.seed.OnboardingFoodCodeSeeder;
import com.example.timetoeat.domain.preference.service.PreferenceCommandService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** client-profile 작업자 spring.patch S1(취향 교체 중복 키 500), S2(음식 코드 시드). */
class PreferenceAndSeedIT extends QaIntegrationSupport {
    @Autowired PreferenceCommandService preferences;
    @Autowired OnboardingFoodCodeSeeder seeder;

    @Test
    void savingTheSamePreferencesAgainDoesNotHitTheUniqueKey() {
        var me = member("재온보딩");
        var req = OnboardingPreferenceRequest.of(List.of("90010001", "90010009"), List.of("90010002"), List.of("90020005"));
        preferences.saveOnboarding(me.getId(), req);
        assertDoesNotThrow(() -> preferences.saveOnboarding(me.getId(), req), "같은 취향 재저장(프로필 수정)");
        preferences.saveOnboarding(me.getId(), OnboardingPreferenceRequest.of(List.of("90010009"), List.of(), List.of()));
        assertEquals(1, jdbc.queryForObject("select count(*) from member_food_preference where member_id=?", Integer.class, me.getId()));
    }

    @Test
    void seederIsIdempotentAndKeepsExistingLabels() throws Exception {
        jdbc.update("update food_code set label='운영에서 바꾼 라벨' where code='90010001'");
        seeder.run(null);
        seeder.run(null);
        assertEquals(21, jdbc.queryForObject("select count(*) from food_code where code like '900%'", Integer.class));
        assertEquals("운영에서 바꾼 라벨", jdbc.queryForObject("select label from food_code where code='90010001'", String.class));
    }
}
