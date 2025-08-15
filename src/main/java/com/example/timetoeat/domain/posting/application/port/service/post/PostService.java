package com.example.timetoeat.domain.posting.application.port.service.post;

import com.example.timetoeat.domain.posting.application.port.in.command.CreatePostCommand;
import com.example.timetoeat.domain.posting.application.port.in.usecase.post.PostUseCase;
import com.example.timetoeat.domain.posting.application.port.out.post.GetPostQuery;
import com.example.timetoeat.domain.posting.application.port.out.post.SavePostPort;
import com.example.timetoeat.domain.posting.application.port.out.postEvent.PostEventPort;
import com.example.timetoeat.domain.posting.domain.model.post.Post;
import com.example.timetoeat.domain.posting.domain.model.post.Status;
import com.example.timetoeat.domain.posting.domain.model.postEvent.PostEvent;
import com.example.timetoeat.domain.posting.domain.model.postEvent.PostEventType;
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
    private final PostEventPort postEventPort;

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
        Post savedPost = savePostPort.save(closedPost);
        PostEvent postEvent = PostEvent.create(savedPost, PostEventType.POST_CLOSED);
        postEventPort.publish(postEvent);
    }

    @Override
    public void expire(PostId postId) {
        Post post = getPostQuery.findById(postId);
        Post expiredPost = post.expire();
        Post savedPost = savePostPort.save(expiredPost);
        PostEvent postEvent = PostEvent.create(savedPost, PostEventType.POST_EXPIRED);
        postEventPort.publish(postEvent);
    }
}
