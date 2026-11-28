package com.example.timetoeat.domain.push.controller;

import com.example.timetoeat.domain.push.dto.PushTokenRequest;
import com.example.timetoeat.domain.push.entity.PushToken;
import com.example.timetoeat.domain.push.repository.PushTokenRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * 기획 문서 §15.3 POST /api/push/subscribe 를 FCM 토큰 방식·인증 사용자 기준으로 바꾼 것.
 * userId 는 본문이 아니라 JWT 에서 가져온다. 임의 발송 API(/api/push/send)는 두지 않는다
 * (§15.3 "발송 API에 서버 또는 관리자 권한" — 발송은 알림 생성 시 서버 내부에서만).
 */
@RestController
@RequestMapping("/api/v1/push/tokens")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PushTokenController {

    static final int MAX_TOKENS_PER_MEMBER = 10;

    private final PushTokenRepository tokens;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void register(@AuthenticationPrincipal(expression = "memberId") Long meId,
                         @Valid @RequestBody PushTokenRequest req) {
        tokens.findByToken(req.token()).ifPresentOrElse(
                t -> { t.reassign(meId, req.platform()); tokens.touch(t.getId(), java.time.LocalDateTime.now()); },
                () -> tokens.save(new PushToken(meId, req.token(), req.platform())));
        // 회원당 기기·브라우저 수만큼만 둔다(보안 재검토 N2). 상한을 넘으면 가장 오래된 것부터 지운다.
        // 상한이 없으면 임의 토큰 수천 개를 올려 알림 한 번에 FCM 호출 수천 번으로 알림 소비자를 묶을 수 있었다.
        var mine = tokens.findByMemberIdOrderByUpdatedAtDescIdDesc(meId);
        if (mine.size() > MAX_TOKENS_PER_MEMBER) {
            tokens.deleteAllInBatch(mine.subList(MAX_TOKENS_PER_MEMBER, mine.size()));
        }
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void unregister(@AuthenticationPrincipal(expression = "memberId") Long meId,
                           @Valid @RequestBody PushTokenRequest req) {
        tokens.deleteByMemberIdAndToken(meId, req.token());
    }
}
