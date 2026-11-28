package com.example.timetoeat.domain.post.application.service;

import com.example.timetoeat.domain.invitation.domain.vo.InvitationId;
import com.example.timetoeat.domain.post.adap.in.dto.PostDetailsDto;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/** Socket S2: 입장 경로 시드만 ?authoritative=true, 이벤트 경로(sendAnnouncement)는 false. 계약 contracts/room-leave-events.md */
class MatchingPostServiceAuthoritativeTest {
    private final List<URI> calls = new CopyOnWriteArrayList<>();
    private final List<String> keys = new CopyOnWriteArrayList<>();
    private final MatchingPostService service = new MatchingPostService(
            WebClient.builder().exchangeFunction(req -> {
                calls.add(req.url());
                keys.add(req.headers().getFirst("X-Room-Key"));
                return Mono.just(ClientResponse.create(HttpStatus.NO_CONTENT).build());
            }).build(),
            "http://socket.invalid/", "room-key");

    private final PostDetailsDto details = new PostDetailsDto("강남역", LocalDateTime.of(2026, 12, 1, 13, 0));

    @Test void entryPathsSendAuthoritativeSeeds() {
        service.ensureAnnouncement(PostId.of(12L), details, List.of(), List.of());
        service.ensureInvitation(new InvitationId(37L), List.of(), List.of());
        assertEquals(URI.create("http://socket.invalid/api/announcement/12?authoritative=true"), calls.get(0));
        assertEquals(URI.create("http://socket.invalid/api/invitation/37?authoritative=true"), calls.get(1));
        assertEquals(List.of("room-key", "room-key"), keys);
    }

    @Test void eventPathSeedIsNotAuthoritative() throws Exception {
        service.sendAnnouncement(PostId.of(13L), details, List.of(), List.of());
        for (int i = 0; i < 50 && calls.isEmpty(); i++) Thread.sleep(20); // subscribe 비동기
        assertEquals(URI.create("http://socket.invalid/api/announcement/13?authoritative=false"), calls.get(0));
    }
}
