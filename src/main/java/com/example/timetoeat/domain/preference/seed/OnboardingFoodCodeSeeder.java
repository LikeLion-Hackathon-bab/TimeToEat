package com.example.timetoeat.domain.preference.seed;

import com.example.timetoeat.domain.preference.entity.FoodCode;
import com.example.timetoeat.domain.preference.repository.FoodCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 온보딩·프로필 수정 화면의 선택지 코드(Client src/apis/foodCodes.ts)를 food_code에 둔다.
 * 없는 코드만 넣으므로 여러 번 실행해도 같다(이미 있는 코드는 라벨도 건드리지 않는다). 90xxxxxx 대역은 AI 음식 분류 코드와 겹치지 않는다.
 * 출처: client-profile 작업자 qa-cloud/client-profile/spring.patch S2. 끄려면 app.seed.food-codes=false.
 */
@Component
@RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.seed.food-codes", havingValue = "true", matchIfMissing = true)
public class OnboardingFoodCodeSeeder implements ApplicationRunner {

    private final FoodCodeRepository foodCodeRepository;

    static final Map<String, String> CODES = new LinkedHashMap<>();
    static {
        CODES.put("90010001", "한식");
        CODES.put("90010002", "중식");
        CODES.put("90010003", "일식");
        CODES.put("90010004", "양식");
        CODES.put("90010005", "아시안식");
        CODES.put("90010006", "분식");
        CODES.put("90010007", "패스트푸드");
        CODES.put("90010008", "채식/비건");
        CODES.put("90010009", "카페/디저트");
        CODES.put("90020001", "계란");
        CODES.put("90020002", "유제품");
        CODES.put("90020003", "과일류");
        CODES.put("90020004", "견과류");
        CODES.put("90020005", "갑각류");
        CODES.put("90020006", "생선");
        CODES.put("90020007", "밀가루");
        CODES.put("90020008", "내장류");
        CODES.put("90020009", "강한 향신료");
        CODES.put("90020010", "비린 해산물");
        CODES.put("90020011", "특수부위");
        CODES.put("90020012", "특이 식감");
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        CODES.forEach((code, label) -> {
            if (!foodCodeRepository.existsById(code)) {
                foodCodeRepository.save(new FoodCode(code, label));
            }
        });
    }
}
