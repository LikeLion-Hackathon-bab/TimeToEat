package com.example.timetoeat.global.auth.demo;

import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.member.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시연이 꺼졌거나 기한이 지난 상태로 기동하면 시연 회원의 refresh 토큰을 지운다(리뷰 D1).
 * 재기동 없이 기한이 지나는 경우는 JwtService.reissue 가 막는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoTokenRevoker implements ApplicationRunner {
    private final DemoLoginWindow window;
    private final MemberJpaRepository members;
    private final RefreshTokenRepository refreshTokens;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (window.isOpen()) return;
        int revoked = 0;
        for (int n = 1; n <= DemoLoginProperties.ACCOUNT_COUNT; n++) {
            var member = members.findByEmail(DemoAccounts.email(n)).orElse(null);
            if (member != null && refreshTokens.findByMemberId(member.getId()).isPresent()) {
                refreshTokens.deleteAllByMemberId(member.getId());
                revoked++;
            }
        }
        if (revoked > 0) log.warn("시연이 닫혀 있어 시연 회원 refresh 토큰 {}개를 지웠습니다.", revoked);
    }
}
