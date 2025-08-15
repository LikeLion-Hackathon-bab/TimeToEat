package com.example.timetoeat.domain.posting.application.port.service.participation;

import com.example.timetoeat.domain.posting.application.port.out.participation.GetParticipationQuery;
import com.example.timetoeat.domain.posting.application.port.out.post.GetPostQuery;
import com.example.timetoeat.domain.posting.application.port.out.participation.SaveParticipationPort;
import com.example.timetoeat.domain.posting.application.port.out.post.SavePostPort;
import com.example.timetoeat.domain.posting.application.port.out.postEvent.PostEventPort;
import com.example.timetoeat.domain.posting.domain.model.postEvent.PostEvent;
import com.example.timetoeat.domain.posting.domain.model.postEvent.PostEventType;
import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import com.example.timetoeat.domain.posting.domain.model.participation.Participation;
import com.example.timetoeat.domain.posting.domain.model.post.Post;
import com.example.timetoeat.domain.posting.domain.vo.PostId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ParticipationTransactionManager {

    private final GetPostQuery getPostQuery;
    private final GetParticipationQuery getParticipationQuery;
    private final SaveParticipationPort saveParticipationPort;
    private final SavePostPort savePostPort;
    private final PostEventPort postEventPort;

    @Transactional
    public void apply(MemberId memberId, PostId postId) {

        Post post = getPostQuery.findById(postId);
        // 공지가 무조건 있어야 되니까 예외 처리 jpa 단에서 해주기

        Participation participation = getParticipationQuery.getParticipationByPostId(post.getPostId());


        if (!post.canApply(participation)) {
            throw new IllegalStateException("이미 마감되었거나 인원이 가득 찬 공고입니다");
        }
        if (participation.contains(memberId)) {
            throw new IllegalStateException("이미 신청한 공고입니다.");
        }

        Participation newParticipation = participation.add(memberId);
        saveParticipationPort.save(newParticipation);

        if (newParticipation.isFull(post.getTargetCount())) {
            Post closedPost = post.close();
            Post savedPost = savePostPort.save(closedPost);
            PostEvent postEvent = PostEvent.create(savedPost, PostEventType.POST_FILLED);
            postEventPort.publish(postEvent);
        }
    }
}
