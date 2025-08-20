package com.example.timetoeat.domain.posting.core.application.post;

import com.example.timetoeat.domain.posting.core.application.port.out.gateway.post.LoadPost;
import com.example.timetoeat.domain.posting.core.application.port.out.gateway.post.SavePost;
import com.example.timetoeat.domain.posting.core.application.port.out.gateway.postEvent.PublishPostEvent;
import com.example.timetoeat.domain.posting.core.application.port.in.usecase.command.CreatePostCommand;
import com.example.timetoeat.domain.posting.core.application.port.in.usecase.usecase.post.PostUseCase;
import com.example.timetoeat.domain.posting.core.domain.model.post.Post;
import com.example.timetoeat.domain.posting.core.domain.model.post.Status;
import com.example.timetoeat.domain.posting.core.domain.model.postEvent.PostEvent;
import com.example.timetoeat.domain.posting.core.domain.model.postEvent.PostEventType;
import com.example.timetoeat.domain.posting.core.domain.vo.member.MemberId;
import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Transactional
@Service
public class PostService implements PostUseCase {

    private final SavePost savePost;
    private final LoadPost loadPost;
    private final PublishPostEvent publishPostEvent;

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

        savePost.save(post);
    }

    @Override
    public void close(MemberId memberId,PostId postId) {
        Post post = loadPost.findById(postId);
        if (!post.isOwnedBy(memberId)) {
            throw new SecurityException("공고를 마감할 권한이 없습니다.");
        }
        Post closedPost = post.close();
        Post savedPost = savePost.save(closedPost);
        PostEvent postEvent = PostEvent.create(savedPost, PostEventType.POST_CLOSED);
        publishPostEvent.publish(postEvent);
    }

    @Override
    public void expire(PostId postId) {
        Post post = loadPost.findById(postId);
        Post expiredPost = post.expire();
        Post savedPost = savePost.save(expiredPost);
        PostEvent postEvent = PostEvent.create(savedPost, PostEventType.POST_EXPIRED);
        publishPostEvent.publish(postEvent);
    }
}
