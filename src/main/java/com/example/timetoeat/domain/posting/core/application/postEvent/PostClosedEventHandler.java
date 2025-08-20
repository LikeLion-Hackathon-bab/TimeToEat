package com.example.timetoeat.domain.posting.core.application.postEvent;

import com.example.timetoeat.domain.posting.core.application.port.out.gateway.subscription.LoadSubscription;
import com.example.timetoeat.domain.posting.core.domain.model.postEvent.PostEvent;
import com.example.timetoeat.domain.posting.core.domain.model.postEvent.PostEventType;
import com.example.timetoeat.domain.posting.core.domain.vo.member.MemberId;
import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PostClosedEventHandler implements PostEventHandler {

    private final LoadSubscription loadSubscription;

    @Override
    public boolean supports(PostEventType eventType) {
        return eventType == PostEventType.POST_CLOSED;
    }

    @Override
    public void handle(PostEvent event) {
        PostId postId = event.postId();
        List<MemberId> memberId = loadSubscription.findMembersByPostId(postId);
    }
}
