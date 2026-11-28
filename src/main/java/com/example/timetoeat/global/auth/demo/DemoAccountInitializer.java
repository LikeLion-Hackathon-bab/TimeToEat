package com.example.timetoeat.global.auth.demo;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.global.auth.model.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/** 기동 시 시연 회원 5명을 없으면 만든다(여러 번 기동해도 한 번만). */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "demo-login", name = "enabled", havingValue = "true")
public class DemoAccountInitializer implements ApplicationRunner {
    private final MemberJpaRepository members;

    @Override
    public void run(ApplicationArguments args) {
        for (int n = 1; n <= DemoLoginProperties.ACCOUNT_COUNT; n++) ensure(n);
        log.warn("시연 로그인이 켜져 있습니다(DEMO_LOGIN_ENABLED=true). 시연이 끝나면 끄세요.");
    }

    void ensure(int n) {
        String email = DemoAccounts.email(n);
        if (members.findByEmail(email).isPresent()) return;
        try {
            members.save(MemberEntity.builder()
                    .username(DemoAccounts.nickname(n))
                    .email(email)
                    .role(Role.ROLE_MEMBER)
                    .build());
            log.info("시연 회원 생성: account={}", n);
        } catch (DataIntegrityViolationException e) {
            // 동시에 다른 인스턴스가 만들었다(email unique)
            log.info("시연 회원이 이미 있음: account={}", n);
        }
    }
}
