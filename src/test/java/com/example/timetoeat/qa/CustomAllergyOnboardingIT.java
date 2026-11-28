package com.example.timetoeat.qa;

import com.example.timetoeat.global.auth.dto.Oauth2UserInfo;
import com.example.timetoeat.global.auth.model.CustomOauth2User;
import com.example.timetoeat.global.auth.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** F07: 온보딩에서 직접 입력한 알레르기(customAllergyLabels)가 저장되고 프로필 상세에서 돌아온다. */
@AutoConfigureMockMvc
class CustomAllergyOnboardingIT extends QaIntegrationSupport {
    @Autowired MockMvc mvc;

    private RequestPostProcessor as(long memberId) {
        var user = new CustomOauth2User(Oauth2UserInfo.fromJwt(memberId, "qa", "qa@qa.test", Role.ROLE_MEMBER));
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private org.springframework.test.web.servlet.ResultActions onboard(long me, String customJson) throws Exception {
        return mvc.perform(post("/api/v1/onboarding").with(as(me)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"userName\":\"알레르기\",\"likedCodes\":[\"90010001\"],\"allergyCodes\":[\"90020005\"]"
                        + (customJson == null ? "" : ",\"customAllergyLabels\":" + customJson) + "}"));
    }

    private org.springframework.test.web.servlet.ResultActions detail(long me) throws Exception {
        return mvc.perform(get("/api/v1/members/me/profile/detail").with(as(me)));
    }

    @Test
    void customAllergiesRoundTripTrimmedAndDeduplicated() throws Exception {
        long me = member("온보딩").getId();
        onboard(me, "[\" 땅콩 \",\"키위\",\"땅콩\",\"  \",\"복숭아\"]").andExpect(status().isNoContent());
        detail(me).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customAllergyLabels", contains("땅콩", "키위", "복숭아")))
                .andExpect(jsonPath("$.data.allergies[0].code").value("90020005"));

        // 다시 저장하면 교체된다(프로필 수정과 같은 규칙). 필드가 없으면 비운다.
        onboard(me, "[\"새우\"]").andExpect(status().isNoContent());
        detail(me).andExpect(jsonPath("$.data.customAllergyLabels", contains("새우")));
        mvc.perform(post("/api/v1/preferences/onboarding").with(as(me)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"allergyCodes\":[],\"customAllergyLabels\":[\"게\"]}"))
                .andExpect(status().isNoContent());
        detail(me).andExpect(jsonPath("$.data.customAllergyLabels", contains("게")));
        onboard(me, null).andExpect(status().isNoContent());
        detail(me).andExpect(jsonPath("$.data.customAllergyLabels", empty()));
    }

    @Test
    void limitsAreEnforcedAfterNormalization() throws Exception {
        long me = member("제한").getId();
        // 공백만 다른 중복은 하나로 센다: 10개 + 중복 1개는 통과
        onboard(me, "[\"a1\",\"a2\",\"a3\",\"a4\",\"a5\",\"a6\",\"a7\",\"a8\",\"a9\",\"a10\",\" a1 \"]")
                .andExpect(status().isNoContent());
        onboard(me, "[\"a1\",\"a2\",\"a3\",\"a4\",\"a5\",\"a6\",\"a7\",\"a8\",\"a9\",\"a10\",\"a11\"]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TOO_MANY_CUSTOM_ALLERGIES"));
        onboard(me, "[\"  " + "가".repeat(20) + "  \"]").andExpect(status().isNoContent());
        onboard(me, "[\"" + "가".repeat(21) + "\"]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CUSTOM_ALLERGY_TOO_LONG"));
        // 실패한 요청은 저장된 값을 바꾸지 않는다
        detail(me).andExpect(jsonPath("$.data.customAllergyLabels", contains("가".repeat(20))));
    }
}
