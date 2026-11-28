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

        announcementRequest(postId, postDetails, paricipations, recentMenus, false)
                .subscribe(
                        response -> log.info("공지 웹소켓 서버로 요청 성공: {}", response.getStatusCode()),
                        error -> log.error("공지 웹소켓 서버로 요청 실패: {}", error.getMessage())
                );
    }

    public void ensureAnnouncement(PostId id, PostDetailsDto details, List<ParticipationSocketDto> people, List<RecentMenuDto> menus) {
        announcementRequest(id, details, people, menus, true).block(java.time.Duration.ofSeconds(4));
    }

    private reactor.core.publisher.Mono<org.springframework.http.ResponseEntity<Void>> announcementRequest(
            PostId postId, PostDetailsDto postDetails, List<ParticipationSocketDto> paricipations, List<RecentMenuDto> recentMenus,
            boolean authoritative) {
        WebSocketRequestDto requestDto = new WebSocketRequestDto(
                String.valueOf(postId.getId()),
                postDetails.location(),
                String.valueOf(postDetails.meetingAt()),
                paricipations,
                recentMenus
        );

        return webClient.post()
                // authoritative=true: 입장 API 가 DB 참여자 목록을 보낸다. Socket 은 없는 참가자를 뺀다(리뷰 S2)
                .uri(socketBaseUrl + "/api/announcement/{announcementId}?authoritative={a}", requestDto.announcementId(), authoritative)
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
                .uri(socketBaseUrl + "/api/invitation/{invitationId}?authoritative=true", requestDto.invitationId())
                .header("X-Room-Key", roomKey)
                .bodyValue(requestDto)
                .retrieve()
                .toBodilessEntity()
                .block(java.time.Duration.ofSeconds(4));
    }

    /**
     * 대기실 나가기 알림(F46). 계약: qa-cloud/contracts/participation-cancel.md
     * 커밋 뒤에 부르고 결과를 기다리지 않는다. 실패는 로그만 남긴다(Spring 상태가 기준이다).
     */
    public void leaveAnnouncement(long postId, long userId, boolean dissolve) {
        leave("/api/announcement/{id}/leave", postId, userId, dissolve);
    }

    public void leaveInvitation(long invitationId, long userId) {
        leave("/api/invitation/{id}/leave", invitationId, userId, true);
    }

    private void leave(String path, long id, long userId, boolean dissolve) {
        webClient.post()
                .uri(socketBaseUrl + path, id)
                .header("X-Room-Key", roomKey)
                .bodyValue(java.util.Map.of("userId", String.valueOf(userId), "dissolve", dissolve))
                .retrieve()
                .toBodilessEntity()
                .timeout(java.time.Duration.ofSeconds(4))
                .subscribe(
                        r -> log.info("대기실 나가기 알림 성공 {} id={} user={} dissolve={}", path, id, userId, dissolve),
                        e -> log.warn("대기실 나가기 알림 실패 {} id={} user={} msg={}", path, id, userId, e.getMessage()));
    }
}
