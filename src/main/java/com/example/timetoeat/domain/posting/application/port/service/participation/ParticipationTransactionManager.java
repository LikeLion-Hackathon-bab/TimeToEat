package com.example.timetoeat.domain.posting.application.port.service.participation;

import com.example.timetoeat.domain.posting.application.port.out.Query.GetParticipationQuery;
import com.example.timetoeat.domain.posting.application.port.out.Query.GetPostQuery;
import com.example.timetoeat.domain.posting.application.port.out.save.SaveParticipationPort;
import com.example.timetoeat.domain.posting.application.port.out.save.SavePostPort;
import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import com.example.timetoeat.domain.posting.domain.model.Participation;
import com.example.timetoeat.domain.posting.domain.model.Post;
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

    @Transactional
    public void apply(MemberId memberId, PostId postId) {

        Post post = getPostQuery.findById(postId);
        // 공지가 무조건 있어야 되니까 예외 처리 jpa 단에서 해주기

        Participation participation = getParticipationQuery.getParticipationByPostId(post.getPostId());
        // participation이 없을 수도 있기 때문에 예외 처리x

        if (!post.canApply(participation)) {
            throw new IllegalStateException("이미 마감되었거나 인원이 가득 찬 공고입니다");
        }
        if (participation.contains(memberId)) {
            throw new IllegalStateException("이미 신청한 공고입니다.");
        }

        Participation newParticipation = participation.add(memberId);
        saveParticipationPort.save(newParticipation);

        if (newParticipation.isFull(post.getTargetCount())) {
            Post newPost = post.close();
            savePostPort.save(newPost);
        }
    }
}
