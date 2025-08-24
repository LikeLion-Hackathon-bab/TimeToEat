package com.example.timetoeat.domain.participation.application.service;

import com.example.timetoeat.domain.participation.application.port.out.LoadParticipation;
import com.example.timetoeat.domain.participation.application.port.out.SaveParticipation;
import com.example.timetoeat.domain.post.application.port.out.LoadPost;
import com.example.timetoeat.domain.post.application.port.out.SavePost;
import com.example.timetoeat.domain.post.application.port.out.PublishPostEvent;
import com.example.timetoeat.domain.post.domain.event.PostCompletedEvent;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.participation.domain.Participation;
import com.example.timetoeat.domain.post.domain.Post;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ParticipationTransactionManager {

    private final LoadPost loadPost;
    private final LoadParticipation loadParticipation;
    private final SaveParticipation saveParticipation;
    private final SavePost savePost;
    private final PublishPostEvent publishPostEvent;

    @Transactional
    public void apply(MemberId memberId, PostId postId) {
        Post post = loadPost.findById(postId);
        Participation participation = loadParticipation.getParticipationByPostId(post.getPostId());


        if (!post.canApply(participation)) {
            throw new IllegalStateException("이미 마감되었거나 인원이 가득 찬 공고입니다");
        }
        if (participation.contains(memberId)) {
            throw new IllegalStateException("이미 신청한 공고입니다.");
        }

        Participation newParticipation = participation.add(memberId);
        saveParticipation.save(newParticipation);

        if (newParticipation.isFull(post.getTargetCount())) {
            Post closedPost = post.close();
            Post savedPost = savePost.save(closedPost);
            publishPostEvent.publish(new PostCompletedEvent(savedPost));
        }
    }
}
