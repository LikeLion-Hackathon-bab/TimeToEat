package com.example.timetoeat.domain.posting.infrastructure.web.subscription;

import com.example.timetoeat.domain.posting.core.application.usecase.usecase.subscription.SubscriptionUseCase;
import com.example.timetoeat.domain.posting.core.domain.vo.member.MemberId;
import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;
import com.example.timetoeat.global.auth.model.CustomOauth2User;
import com.example.timetoeat.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscribe/{postId}")
@RequiredArgsConstructor
@Tag(name = "공고 구독 API")
public class SubscriptionController {
    private final SubscriptionUseCase subscriptionUseCase;

    @PostMapping
    @Operation(summary = "공지 구독 API")
    public ApiResponse<Object> subscribe(
        @AuthenticationPrincipal CustomOauth2User customOauth2User,
        @PathVariable Long postId
    )
    {
        if (customOauth2User == null) {
            throw new IllegalStateException("로그인이 필요한 서비스입니다.");
        }
        MemberId memberId = new MemberId(customOauth2User.getMemberId());
        subscriptionUseCase.subscribe(memberId, new PostId(postId));

        return ApiResponse.success("공고 구독 완료.");
    }

    //공지 구독한 사람 조회하는 api 추가하기
}
