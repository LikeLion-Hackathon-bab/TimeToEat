package com.example.timetoeat.domain.post.application.service;

import com.example.timetoeat.domain.invitation.adap.in.dto.InvitationSocketRequestDto;
import com.example.timetoeat.domain.invitation.domain.vo.InvitationId;
import com.example.timetoeat.domain.participation.adap.in.dto.ParticipationSocketDto;
import com.example.timetoeat.domain.post.adap.in.dto.PostDetailsDto;
import com.example.timetoeat.domain.post.adap.in.dto.RecentMenuDto;
import com.example.timetoeat.domain.post.adap.in.dto.WebSocketRequestDto;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@Slf4j
public class MatchingPostService {

    private final WebClient webClient;
    private final String socketBaseUrl;
    private final String roomKey;

    public MatchingPostService(WebClient webClient, @Value("${matching.socket-base-url}") String socketBaseUrl, @Value("${ROOM_RESULT_KEY:}") String roomKey) {
        this.roomKey = roomKey;
        this.webClient = webClient;
        this.socketBaseUrl = socketBaseUrl.replaceFirst("/+$", "");
    }

    public void sendAnnouncement(PostId postId,
                                 PostDetailsDto postDetails,
                                 List<ParticipationSocketDto> paricipations,
                                 List<RecentMenuDto> recentMenus) {

        announcementRequest(postId, postDetails, paricipations, recentMenus)
                .subscribe(
                        response -> log.info("공지 웹소켓 서버로 요청 성공: {}", response.getStatusCode()),
                        error -> log.error("공지 웹소켓 서버로 요청 실패: {}", error.getMessage())
                );
    }

    public void ensureAnnouncement(PostId id, PostDetailsDto details, List<ParticipationSocketDto> people, List<RecentMenuDto> menus) {
        announcementRequest(id, details, people, menus).block(java.time.Duration.ofSeconds(4));
    }

    private reactor.core.publisher.Mono<org.springframework.http.ResponseEntity<Void>> announcementRequest(
            PostId postId, PostDetailsDto postDetails, List<ParticipationSocketDto> paricipations, List<RecentMenuDto> recentMenus) {
        WebSocketRequestDto requestDto = new WebSocketRequestDto(
                String.valueOf(postId.getId()),
                postDetails.location(),
                String.valueOf(postDetails.meetingAt()),
                paricipations,
                recentMenus
        );

        return webClient.post()
                .uri(socketBaseUrl + "/api/announcement/{announcementId}", requestDto.announcementId())
                .header("X-Room-Key", roomKey)
                .bodyValue(requestDto)
                .retrieve()
                .toBodilessEntity();
    }

    public void ensureInvitation(InvitationId invitationId,
                               List<ParticipationSocketDto> participations,
                               List<RecentMenuDto> recentMenus) {

        InvitationSocketRequestDto requestDto = new InvitationSocketRequestDto(
                String.valueOf(invitationId.id()),
                participations,
                recentMenus
        );

        webClient.post()
                .uri(socketBaseUrl + "/api/invitation/{invitationId}", requestDto.invitationId())
                .header("X-Room-Key", roomKey)
                .bodyValue(requestDto)
                .retrieve()
                .toBodilessEntity()
                .block(java.time.Duration.ofSeconds(4));
    }
}
