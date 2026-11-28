package com.example.timetoeat.domain.post.application.service;

import com.example.timetoeat.domain.article.service.ArticleMealQueryService;
import com.example.timetoeat.domain.participation.adap.in.dto.ParticipationSocketDto;
import com.example.timetoeat.domain.participation.application.port.out.LoadParticipation;
import com.example.timetoeat.domain.participation.application.service.ParticipationInfoService;
import com.example.timetoeat.domain.participation.domain.Participation;
import com.example.timetoeat.domain.post.adap.in.dto.PostDetailsDto;
import com.example.timetoeat.domain.post.adap.in.dto.RecentMenuDto;
import com.example.timetoeat.domain.post.domain.event.PostExpiredEvent;
import com.example.timetoeat.domain.subscription.application.port.out.LoadSubscription;
import com.example.timetoeat.domain.post.domain.PostEventType;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import com.example.timetoeat.global.common.EventHandler;
import com.example.timetoeat.global.util.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;


@Component
@RequiredArgsConstructor
public class ExpiredEventHandler implements EventHandler<PostExpiredEvent> {

    private final LoadParticipation loadParticipation;
    private final ArticleMealQueryService articleMealQueryService;
    private final ParticipationInfoService participationInfoService;
    private final RecentMenuService recentMenuService;
    private final MatchingPostService matchingPostService;
    private final PostParsingService postParsingService;

    @Override
    public boolean supports(DomainEvent event) {
        return event instanceof PostExpiredEvent;
    }

    @Override
    public void handle(PostExpiredEvent event) {
        PostId postId = event.post().getPostId();
        PostDetailsDto postDetails = postParsingService.getPostDetails(event.post());

        Participation participation = loadParticipation.getParticipationByPostId(postId);
        // 참여자 없이 만료된 공고는 CLOSED만 하고 대기실을 만들지 않는다. 작성자 혼자인 방은 대기실 목록
        // (PostRepository.findWaitingRooms: 참여 행 필요)에도 안 보이고 알림(NotificationEventRecorder)도 보내지 않는다.
        if (participation.getMemberIds().isEmpty()) return;
        Set<MemberId> memberIds = new java.util.LinkedHashSet<>(participation.getMemberIds());
        memberIds.add(event.post().getMemberId());
        List<ParticipationSocketDto> participations = participationInfoService.getParticipations(memberIds);
        List<RecentMenuDto> recentMenus = recentMenuService.getRecentMenus(memberIds);

        matchingPostService.sendAnnouncement(postId,postDetails,participations,recentMenus);
    }
}
