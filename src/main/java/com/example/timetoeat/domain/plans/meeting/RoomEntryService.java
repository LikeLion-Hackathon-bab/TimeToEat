package com.example.timetoeat.domain.plans.meeting;

import java.util.LinkedHashSet;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.example.timetoeat.domain.post.adap.out.persistence.PostRepository;
import com.example.timetoeat.domain.post.adap.in.dto.PostDetailsDto;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.participation.application.port.out.LoadParticipation;
import com.example.timetoeat.domain.participation.application.service.ParticipationInfoService;
import com.example.timetoeat.domain.post.application.service.MatchingPostService;
import com.example.timetoeat.domain.post.application.service.RecentMenuService;

@Service
@RequiredArgsConstructor
class RoomEntryService {
    private final PostRepository posts;
    private final LoadParticipation participants;
    private final ParticipationInfoService profiles;
    private final RecentMenuService menus;
    private final MatchingPostService socket;

    public void enter(long id, Long viewer) {
        if (viewer == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var post = posts.findWaitingRooms(viewer).stream().filter(p -> p.getId().equals(id)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var postId = PostId.of(id);
        var members = new LinkedHashSet<>(participants.getParticipationByPostId(postId).getMemberIds());
        members.add(new MemberId(post.getMember().getId()));
        var details = new PostDetailsDto(post.getLocation(), post.getMeetingAt());
        var people = profiles.getParticipations(members);
        var recent = menus.getRecentMenus(members);
        try {
            socket.ensureAnnouncement(postId, details, people, recent);
        } catch (RuntimeException unavailable) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "대기실 연결을 다시 시도해 주세요", unavailable);
        }
    }
}
