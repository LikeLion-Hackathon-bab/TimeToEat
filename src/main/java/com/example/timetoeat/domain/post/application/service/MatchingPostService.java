package com.example.timetoeat.domain.post.application.service;

import com.example.timetoeat.domain.participation.adap.in.dto.ParticipationSocketDto;
import com.example.timetoeat.domain.post.adap.in.dto.PostDetailsDto;
import com.example.timetoeat.domain.post.adap.in.dto.RecentMenuDto;
import com.example.timetoeat.domain.post.adap.in.dto.WebSocketRequestDto;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MatchingPostService {

    private final WebClient webClient;
    private static final String ANNOUNCEMENT_URL = "http://13.125.49.103:3000/api/announcement/{announcementId}";

    public void sendAnnouncement(PostId postId,
                                 PostDetailsDto postDetails,
                                 List<ParticipationSocketDto> paricipations,
                                 List<RecentMenuDto> recentMenus) {

        WebSocketRequestDto requestDto = new WebSocketRequestDto(
                String.valueOf(postId.getId()),
                postDetails.location(),
                String.valueOf(postDetails.meetingAt()),
                paricipations,
                recentMenus
        );

        webClient.post()
                .uri(ANNOUNCEMENT_URL, requestDto.announcementId())
                .bodyValue(requestDto)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        response -> log.info("웹소켓 서버로 요청 성공: {}", response.getStatusCode()),
                        error -> log.error("웹소켓 서버로 요청 실패: {}", error.getMessage())
                );
    }
}
