package com.example.timetoeat.domain.post.application.service;

import com.example.timetoeat.domain.post.application.port.out.LoadPost;
import com.example.timetoeat.domain.post.application.port.out.SavePost;
import com.example.timetoeat.domain.post.application.port.out.PublishPostEvent;
import com.example.timetoeat.domain.post.application.port.in.command.CreatePostCommand;
import com.example.timetoeat.domain.post.application.port.in.usecase.PostUseCase;
import com.example.timetoeat.domain.post.domain.Post;
import com.example.timetoeat.domain.post.domain.Status;
import com.example.timetoeat.domain.post.domain.PostEvent;
import com.example.timetoeat.domain.post.domain.PostEventType;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
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
