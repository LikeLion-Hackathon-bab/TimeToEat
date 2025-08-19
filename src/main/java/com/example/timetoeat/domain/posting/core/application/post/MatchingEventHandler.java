package com.example.timetoeat.domain.posting.core.application.post;

import com.example.timetoeat.domain.posting.core.application.participation.ParticipationProvider;
import com.example.timetoeat.domain.posting.core.application.postEvent.PostEventHandler;
import com.example.timetoeat.domain.posting.core.domain.model.postEvent.PostEvent;
import com.example.timetoeat.domain.posting.core.domain.model.postEvent.PostEventType;
import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MatchingEventHandler implements PostEventHandler {

    private final ParticipationProvider participationProvider;

    @Override
    public boolean supports(PostEventType eventType) {
        return List.of(PostEventType.POST_CLOSED,
                       PostEventType.POST_EXPIRED,
                       PostEventType.POST_FILLED).contains(eventType);
    }

    @Override
    public void handle(PostEvent event) {
        PostId postId = new PostId(event.postId().getId());
        Set<Long> participantIds = participationProvider.getParticipants(postId);


    }
}
