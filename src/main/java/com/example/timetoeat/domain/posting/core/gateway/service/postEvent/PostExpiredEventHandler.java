package com.example.timetoeat.domain.posting.core.gateway.service.postEvent;

import com.example.timetoeat.domain.posting.core.gateway.service.out.subscription.GetSubscriptionPort;
import com.example.timetoeat.domain.posting.domain.model.postEvent.PostEvent;
import com.example.timetoeat.domain.posting.domain.model.postEvent.PostEventType;
import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import com.example.timetoeat.domain.posting.domain.vo.PostId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
public class PostExpiredEventHandler implements PostEventHandler {

    private final GetSubscriptionPort getSubscriptionPort;

    @Override
    public boolean supports(PostEventType eventType) {
        return eventType == PostEventType.POST_EXPIRED;
    }

    @Override
    public void handle(PostEvent event) {
        PostId postId = event.postId();
        List<MemberId> memberId = getSubscriptionPort.findMembersByPostId(postId);
    }
}
