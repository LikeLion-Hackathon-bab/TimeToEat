package com.example.timetoeat.domain.posting.application.port.service;

import com.example.timetoeat.domain.posting.adapter.out.mapper.PostMapper;
import com.example.timetoeat.domain.posting.application.port.in.command.CreatePostCommand;
import com.example.timetoeat.domain.posting.application.port.in.usecase.PostUseCase;
import com.example.timetoeat.domain.posting.application.port.out.Query.GetPostQuery;
import com.example.timetoeat.domain.posting.application.port.out.save.SavePostPort;
import com.example.timetoeat.domain.posting.domain.model.*;
import com.example.timetoeat.domain.posting.domain.vo.PostId;
import com.example.timetoeat.domain.posting.dto.request.PostReq;
import com.example.timetoeat.global.auth.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Transactional
public class PostService implements PostUseCase {

    private final SavePostPort savePostPort;
    private final GetPostQuery getPostQuery;
    private final PostMapper postMapper;

    @Override
    public void create(Member member, PostReq postReq) {
        CreatePostCommand command = new CreatePostCommand(
                postReq.targetCount(),
                postReq.meetingAt(),
                postReq.location(),
                postReq.message()
        );
        command.validateSelf(); //유효성 검증

        Post post = Post.withoutId(
                command.getTargetCount(),
                Status.OPEN,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(2),
                command.getMeetingAt(),
                command.getLocation(),
                command.getMessage()
        );

        savePostPort.save(post);
    }

    @Override
    public void close(PostId postId) {
        Post post = getPostQuery.findById(postId);
        Post closedPost = post.close();
        savePostPort.save(closedPost);
        // 필요하면 마감 알림, 이벤트 등 발송 가능
    }

    @Override
    public void expire(PostId postId) {
        Post post = getPostQuery.findById(postId);
        Post expiredPost = post.expire();
        savePostPort.save(expiredPost);
    }
}
