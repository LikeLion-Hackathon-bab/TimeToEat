package com.example.timetoeat.domain.posting.core.application.service.post;

import com.example.timetoeat.domain.posting.core.application.gateway.participation.GetParticipationQuery;
import com.example.timetoeat.domain.posting.core.application.gateway.post.GetPostQuery;
import com.example.timetoeat.domain.posting.core.application.service.postEvent.PostEventHandler;
import com.example.timetoeat.domain.posting.core.domain.model.participation.Participation;
import com.example.timetoeat.domain.posting.core.domain.model.post.Post;
import com.example.timetoeat.domain.posting.core.domain.model.postEvent.PostEvent;
import com.example.timetoeat.domain.posting.core.domain.model.postEvent.PostEventType;
import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MatchingEventHandler implements PostEventHandler {

    private final GetParticipationQuery getParticipationQuery;
    private final GetPostQuery getPostQuery;

    @Override
    public boolean supports(PostEventType eventType) {
        return List.of(PostEventType.POST_CLOSED,
                       PostEventType.POST_EXPIRED,
                       PostEventType.POST_FILLED).contains(eventType);
    }

    @Override
    public void handle(PostEvent event) {
        PostId postId = new PostId(event.postId().getId());

        Participation participation = getParticipationQuery.getParticipationByPostId(postId);
        Post post = getPostQuery.findById(postId);

        Set<Long> memberIds = participation.getMemberIds().stream()
                .map(memberId -> memberId.getId())
                .collect(Collectors.toSet());

        memberIds.add(post.getMemberId().getId());

        if (memberIds.isEmpty()) {
            throw new IllegalStateException("참여자는 비어 있을 수 없습니다");
        }

    }
}
