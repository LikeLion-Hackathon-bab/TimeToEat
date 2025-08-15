package com.example.timetoeat.domain.posting.application.port.service.post;

import com.example.timetoeat.domain.posting.application.port.in.command.CreatePostCommand;
import com.example.timetoeat.domain.posting.application.port.in.usecase.PostUseCase;
import com.example.timetoeat.domain.posting.application.port.out.Query.GetPostQuery;
import com.example.timetoeat.domain.posting.application.port.out.save.SavePostPort;
import com.example.timetoeat.domain.posting.domain.model.*;
import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import com.example.timetoeat.domain.posting.domain.vo.PostId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Transactional
@Service
public class PostService implements PostUseCase {

    private final SavePostPort savePostPort;
    private final GetPostQuery getPostQuery;

    @Override
    public void create(MemberId memberId, CreatePostCommand command) {
        Post post = Post.withoutId(
                memberId,
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
    public void close(MemberId memberId,PostId postId) {
        Post post = getPostQuery.findById(postId);
        if (!post.isOwnedBy(memberId)) {
            throw new SecurityException("공고를 마감할 권한이 없습니다.");
        }
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
