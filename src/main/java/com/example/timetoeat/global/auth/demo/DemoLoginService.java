package com.example.timetoeat.global.auth.demo;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.member.repository.SocialAccountRepository;
import com.example.timetoeat.domain.preference.repository.MemberFoodPreferenceRepository;
import com.example.timetoeat.global.auth.dto.TokenDto;
import com.example.timetoeat.global.auth.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "demo-login", name = "enabled", havingValue = "true")
public class DemoLoginService {
    private final DemoLoginProperties properties;
    private final MemberJpaRepository members;
    private final SocialAccountRepository socialAccounts;
    private final MemberFoodPreferenceRepository preferences;
    private final JwtService jwtService;

    public enum Outcome { OK, EXPIRED, BAD_SECRET, BAD_ACCOUNT, NOT_DEMO_MEMBER }

    public record Result(Outcome outcome, TokenDto tokens, boolean signupRequired) {
        static Result of(Outcome o) { return new Result(o, null, false); }
    }

    public boolean expired() { return properties.expired(); }

    @Transactional
    public Result login(String secret, Integer account) {
        if (properties.expired()) return Result.of(Outcome.EXPIRED);
        if (!secretMatches(secret)) return Result.of(Outcome.BAD_SECRET);
        if (!DemoAccounts.valid(account)) return Result.of(Outcome.BAD_ACCOUNT);

        MemberEntity member = members.findByEmail(DemoAccounts.email(account)).orElse(null);
        // 시연 회원은 소셜 계정이 없다. 소셜 계정이 붙은 회원(실제 사용자)은 어떤 경우에도 고르지 않는다.
        if (member == null || socialAccounts.existsByMemberEntity_Id(member.getId())) {
            return Result.of(Outcome.NOT_DEMO_MEMBER);
        }
        TokenDto tokens = jwtService.issueForMember(member.getId());
        // OAuth 첫 로그인과 같이, 선호 입력 전이면 온보딩으로 보낸다.
        boolean signupRequired = !preferences.existsByMember_Id(member.getId());
        return new Result(Outcome.OK, tokens, signupRequired);
    }

    /** 길이가 달라도 같은 시간: 양쪽을 SHA-256으로 줄인 뒤 MessageDigest.isEqual. 컨트롤러가 제한 판단에도 쓴다. */
    public boolean secretMatches(String given) {
        if (given == null) return false;
        return MessageDigest.isEqual(sha256(properties.secret()), sha256(given.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] sha256(byte[] v) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(v);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
