package com.example.timetoeat.domain.post.application.service;

import com.example.timetoeat.domain.subscription.application.port.out.LoadSubscription;
import com.example.timetoeat.domain.post.domain.PostEvent;
import com.example.timetoeat.domain.post.domain.PostEventType;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
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
