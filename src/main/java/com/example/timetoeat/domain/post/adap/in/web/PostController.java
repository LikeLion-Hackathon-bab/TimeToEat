package com.example.timetoeat.domain.post.adap.in.web;

import com.example.timetoeat.domain.post.application.port.in.command.CreatePostCommand;
import com.example.timetoeat.domain.post.application.port.in.usecase.GetPostUseCase;
import com.example.timetoeat.domain.post.application.port.in.usecase.PostUseCase;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import com.example.timetoeat.domain.post.application.port.dto.request.PostReq;
import com.example.timetoeat.domain.post.application.port.dto.response.PostRes;
import com.example.timetoeat.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Tag(name = "공지 API")
public class PostController {

    private final PostUseCase postUseCase;
    private final GetPostUseCase getPostUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "공지 작성 API")
    public ApiResponse<Object> createPost(
            @AuthenticationPrincipal(expression = "memberId") Long memberId,
            @RequestBody PostReq postReq
    ) {
        if (memberId == null) {
            throw new IllegalStateException("로그인이 필요한 서비스입니다.");
        }

        CreatePostCommand command = new CreatePostCommand(
                postReq.targetCount(),
                postReq.meetingAt(),
                postReq.location(),
                postReq.message()
        );
        command.validateSelf();

        postUseCase.create(new MemberId(memberId), command);

        return ApiResponse.success("공고가 성공적으로 등록되었습니다.");
    }

    @PostMapping("/{postId}/close")
    @Operation(summary = "공지 마감 API")
    public ApiResponse<Object> closePost(
            @AuthenticationPrincipal(expression = "memberId") Long memberId,
            @PathVariable Long postId
    ) {
        if (memberId == null) {
            throw new IllegalStateException("로그인이 필요한 서비스입니다.");
        }
        String result = postUseCase.close(new MemberId(memberId), new PostId(postId));

        return ApiResponse.success(result);
    }

    @GetMapping
    @Operation(summary = "공지 전체 조회 API")
    public ApiResponse<List<PostRes>> getPosts()
    {
        List<PostRes> posts = getPostUseCase.getPosts();
        return ApiResponse.success(posts);
    }
}
