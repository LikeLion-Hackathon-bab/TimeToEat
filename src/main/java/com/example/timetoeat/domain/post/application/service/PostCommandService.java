package com.example.timetoeat.domain.post.application.service;

import com.example.timetoeat.domain.post.application.port.out.DeletePost;
import com.example.timetoeat.domain.post.application.port.out.LoadPost;
import com.example.timetoeat.domain.post.application.port.out.SavePost;
import com.example.timetoeat.domain.post.application.port.out.PublishPostEvent;
import com.example.timetoeat.domain.post.application.port.in.command.CreatePostCommand;
import com.example.timetoeat.domain.post.application.port.in.usecase.PostUseCase;
import com.example.timetoeat.domain.post.domain.Post;
import com.example.timetoeat.domain.post.domain.Status;
import com.example.timetoeat.domain.post.domain.event.PostClosedEvent;
import com.example.timetoeat.domain.post.domain.event.PostExpiredEvent;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Transactional
@Service
public class PostCommandService implements PostUseCase {

    private final SavePost savePost;
    private final LoadPost loadPost;
    private final PublishPostEvent publishPostEvent;
    private final DeletePost deletePost;

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
    public String close(MemberId memberId,PostId postId) {
        Post post = loadPost.findForUpdate(postId);
        if (!post.isOwnedBy(memberId)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "공고를 마감할 권한이 없습니다.");
        }
        if (post.getStatus() != Status.OPEN) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, "이미 마감된 공고입니다.");
        Post closedPost = post.close();
        Post savedPost = savePost.save(closedPost);
        publishPostEvent.publish(new PostClosedEvent(savedPost));
        return String.valueOf(savedPost.getPostId().getId());
    }

    @Override
    public boolean expire(PostId postId) {
        // 행 잠금(findForUpdate) 뒤 상태를 다시 본다. 참여 시 지연 만료(ParticipationTransactionManager, 410)나
        // 다른 스윕이 먼저 마감했으면 이벤트를 다시 내지 않는다(대기실 생성 중복 방지).
        Post post = loadPost.findForUpdate(postId);
        if (post.getStatus() != Status.OPEN) return false;
        if (post.getExpiredAt().isAfter(LocalDateTime.now())) return false;
        Post savedPost = savePost.save(post.expire());
        publishPostEvent.publish(new PostExpiredEvent(savedPost));
        return true;
    }

    // 공고 삭제: 작성자만, 모집 중(OPEN)이고 아직 참여자가 없을 때만 허용한다.
    @Override
    public void delete(MemberId memberId, PostId postId) {
        Post post = loadPost.findForUpdate(postId);
        if (!post.isOwnedBy(memberId)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "공고를 삭제할 권한이 없습니다.");
        }
        if (post.getStatus() != Status.OPEN) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, "마감된 공고는 삭제할 수 없습니다.");
        if (deletePost.hasParticipants(postId)) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, "참여자가 있는 공고는 삭제할 수 없습니다.");
        deletePost.delete(postId);
    }
}
