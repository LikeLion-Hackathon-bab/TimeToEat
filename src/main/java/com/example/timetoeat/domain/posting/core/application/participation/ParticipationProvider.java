package com.example.timetoeat.domain.posting.core.application.participation;

import com.example.timetoeat.domain.posting.core.application.port.out.gateway.participation.GetParticipationQuery;
import com.example.timetoeat.domain.posting.core.application.port.out.gateway.post.GetPostQuery;
import com.example.timetoeat.domain.posting.core.domain.model.participation.Participation;
import com.example.timetoeat.domain.posting.core.domain.model.post.Post;
import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ParticipationProvider {
    private final GetParticipationQuery getParticipationQuery;
    private final GetPostQuery getPostQuery;

    public Set<Long> getParticipants(PostId postId) {
        Participation participation = getParticipationQuery.getParticipationByPostId(postId);
        Post post = getPostQuery.findById(postId);

        Set<Long> memberIds = participation.getMemberIds().stream()
                .map(memberId -> memberId.getId())
                .collect(Collectors.toSet());

        memberIds.add(post.getMemberId().getId());

        if (memberIds.isEmpty()) {
            throw new IllegalStateException("참여자는 비어 있을 수 없습니다");
        }
        return memberIds;
    }
}
