package com.example.timetoeat.qa;

import com.example.timetoeat.domain.participation.application.port.in.usecase.ParticipationUseCase;
import com.example.timetoeat.domain.plans.meeting.RoomLeaveService;
import com.example.timetoeat.domain.post.application.port.in.usecase.GetPostUseCase;
import com.example.timetoeat.domain.post.application.port.in.usecase.PostUseCase;
import com.example.timetoeat.domain.post.application.service.PostExpirySweeper;
import com.example.timetoeat.domain.post.domain.Status;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import com.example.timetoeat.global.auth.dto.Oauth2UserInfo;
import com.example.timetoeat.global.auth.model.CustomOauth2User;
import com.example.timetoeat.global.auth.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 검증 14 O1: close()가 expiredAt을 덮어써 Client가 '마감'과 '기간 만료'를 구분할 수 없었다.
 * PostRes에 status·closeReason을 더하고(기존 필드 유지), 단건 조회로 마감된 공고의 사유를 볼 수 있게 한다.
 */
@AutoConfigureMockMvc
class PostCloseReasonIT extends QaIntegrationSupport {
    @Autowired MockMvc mvc;
    @Autowired PostUseCase postUseCase;
    @Autowired GetPostUseCase getPostUseCase;
    @Autowired ParticipationUseCase participation;
    @Autowired PostExpirySweeper sweeper;
    @Autowired RoomLeaveService roomLeave;

    private String reasonInDb(long id) {
        return jdbc.queryForObject("select close_reason from post where id=?", String.class, id);
    }

    @Test
    void openPostHasOpenStatusAndNoReasonInListAndDetail() {
        var owner = member("모집중");
        var post = openPost(owner, 2);
        var listed = getPostUseCase.getPosts().stream().filter(p -> p.postId().equals(post.getId())).findFirst().orElseThrow();
        assertEquals("OPEN", listed.status());
        assertNull(listed.closeReason());
        assertNotNull(listed.expiredAt());
        var detail = getPostUseCase.getPost(new PostId(post.getId()));
        assertEquals("OPEN", detail.status());
        assertNull(detail.closeReason());
    }

    @Test
    void manualCloseIsDistinguishableFromExpiry() {
        var owner = member("마감자");
        var manual = openPost(owner, 2);
        postUseCase.close(new MemberId(owner.getId()), new PostId(manual.getId()));

        var expired = openPost(owner, 2);
        jdbc.update("update post set expired_at = now() - interval 1 minute where id=?", expired.getId());
        sweeper.sweep();

        var m = getPostUseCase.getPost(new PostId(manual.getId()));
        var e = getPostUseCase.getPost(new PostId(expired.getId()));
        assertEquals("CLOSED", m.status());
        assertEquals("MANUAL", m.closeReason());
        assertEquals("CLOSED", e.status());
        assertEquals("EXPIRED", e.closeReason());
        assertEquals("MANUAL", reasonInDb(manual.getId()));
        assertEquals("EXPIRED", reasonInDb(expired.getId()));
    }

    @Test
    void fullPostIsFullAndReopenClearsTheReason() {
        var owner = member("모집자");
        var joiner = member("참여자");
        var post = openPost(owner, 1);
        participation.apply(new MemberId(joiner.getId()), new PostId(post.getId()));
        var full = getPostUseCase.getPost(new PostId(post.getId()));
        assertEquals("CLOSED", full.status());
        assertEquals("FULL", full.closeReason());

        // 마지막 참여자가 나가면 다시 모집(OPEN): 사유를 지운다
        assertEquals(RoomLeaveService.Outcome.LEFT_AND_REOPENED, roomLeave.leaveAnnouncement(joiner.getId(), post.getId()));
        assertNull(reasonInDb(post.getId()));
        var reopened = getPostUseCase.getPost(new PostId(post.getId()));
        assertEquals("OPEN", reopened.status());
        assertNull(reopened.closeReason());
    }

    @Test
    void staleOpenPostPastExpiryIsReportedAsExpiredBeforeTheSweep() {
        var owner = member("지연");
        var stale = savePost(owner, 2, Status.OPEN, LocalDateTime.now().minusSeconds(5), LocalDateTime.now().plusDays(1));
        var res = getPostUseCase.getPost(new PostId(stale.getId()));
        assertEquals("CLOSED", res.status());
        assertEquals("EXPIRED", res.closeReason());
        assertEquals("OPEN", jdbc.queryForObject("select status from post where id=?", String.class, stale.getId()));
    }

    @Test
    void detailEndpointReturnsStatusAndReasonAndKeepsExistingFields() throws Exception {
        var owner = member("상세");
        var post = openPost(owner, 2);
        postUseCase.close(new MemberId(owner.getId()), new PostId(post.getId()));
        var user = new CustomOauth2User(Oauth2UserInfo.fromJwt(owner.getId(), "qa", "qa@qa.test", Role.ROLE_MEMBER));
        mvc.perform(get("/api/v1/posts/{id}", post.getId())
                        .with(authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.postId").value(post.getId()))
                .andExpect(jsonPath("$.data.expiredAt").exists())
                .andExpect(jsonPath("$.data.targetCount").value(2))
                .andExpect(jsonPath("$.data.author.authorId").value(owner.getId()))
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.closeReason").value("MANUAL"));
        mvc.perform(get("/api/v1/posts/{id}", 987654321L)
                        .with(authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()))))
                .andExpect(status().isNotFound());
    }
}
