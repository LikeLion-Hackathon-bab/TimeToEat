package com.example.timetoeat.qa;

import com.example.timetoeat.domain.article.entity.Article;
import com.example.timetoeat.domain.article.entity.ArticleTag;
import com.example.timetoeat.domain.article.repository.ArticleRepository;
import com.example.timetoeat.domain.article.repository.ArticleTagRepository;
import com.example.timetoeat.domain.invitation.adap.out.persistence.InvitationEntity;
import com.example.timetoeat.domain.invitation.adap.out.persistence.InvitationRepository;
import com.example.timetoeat.domain.invitation.domain.InvitationStatus;
import com.example.timetoeat.domain.participation.application.port.in.usecase.ParticipationUseCase;
import com.example.timetoeat.domain.post.domain.Status;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import com.example.timetoeat.domain.subscription.adap.out.persistence.SubscriptionEntity;
import com.example.timetoeat.domain.subscription.adap.out.persistence.SubscriptionRepository;
import com.example.timetoeat.global.auth.dto.Oauth2UserInfo;
import com.example.timetoeat.global.auth.model.CustomOauth2User;
import com.example.timetoeat.global.auth.model.Role;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** client-actions 작업자 spring.patch(공고 삭제, 초대장 message, 게시글 taggedMemberIds) + 없는 경로 404. */
@AutoConfigureMockMvc
class ClientActionsIT extends QaIntegrationSupport {
    @Autowired MockMvc mvc;
    @Autowired SubscriptionRepository subscriptions;
    @Autowired InvitationRepository invitations;
    @Autowired ArticleRepository articles;
    @Autowired ArticleTagRepository tags;
    @Autowired ParticipationUseCase participation;
    @Autowired com.example.timetoeat.domain.post.application.port.in.usecase.PostUseCase postUseCase;

    private RequestPostProcessor as(long memberId) {
        var user = new CustomOauth2User(Oauth2UserInfo.fromJwt(memberId, "qa", "qa@qa.test", Role.ROLE_MEMBER));
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    private int postRows(long id) {
        return jdbc.queryForObject("select count(*) from post where id=?", Integer.class, id);
    }

    @Test
    void deletePostContract() throws Exception {
        var owner = member("모집자");
        var other = member("남");
        var p = openPost(owner, 2);
        subscriptions.save(new SubscriptionEntity(p, other));
        mvc.perform(delete("/api/v1/posts/" + p.getId()).with(as(other.getId()))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/posts/" + p.getId()).with(as(owner.getId()))).andExpect(status().isNoContent());
        assertEquals(0, postRows(p.getId()));
        assertEquals(0, jdbc.queryForObject("select count(*) from subscription where post_id=?", Integer.class, p.getId()));
        mvc.perform(delete("/api/v1/posts/" + p.getId()).with(as(owner.getId()))).andExpect(status().isNotFound());

        var withPeople = openPost(owner, 3);
        participation.apply(new MemberId(other.getId()), new PostId(withPeople.getId()));
        mvc.perform(delete("/api/v1/posts/" + withPeople.getId()).with(as(owner.getId())))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("참여자가 있는 공고는 삭제할 수 없습니다."));
        assertEquals(1, postRows(withPeople.getId()));

        var closed = savePost(owner, 2, Status.CLOSED, LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        mvc.perform(delete("/api/v1/posts/" + closed.getId()).with(as(owner.getId()))).andExpect(status().isConflict());
    }

    /** 삭제와 첫 신청이 동시에 와도 '삭제됨 + 참여 행 남음'(고아) 또는 둘 다 성공은 없다. 공고 행 잠금으로 직렬화. */
    @RepeatedTest(5)
    void deleteRacesWithFirstApplication() throws Exception {
        var owner = member("모집자");
        var friend = member("친구");
        var p = openPost(owner, 2);
        var pool = Executors.newFixedThreadPool(2);
        var gate = new CountDownLatch(1);
        Callable<Integer> apply = () -> { gate.await(); try { participation.apply(new MemberId(friend.getId()), new PostId(p.getId())); return 200; }
            catch (ResponseStatusException e) { return e.getStatusCode().value(); } catch (RuntimeException e) { return 404; } };
        Callable<Integer> del = () -> { gate.await(); try { postUseCase.delete(new MemberId(owner.getId()), new PostId(p.getId())); return 204; }
            catch (ResponseStatusException e) { return e.getStatusCode().value(); } };
        var fa = pool.submit(apply); var fd = pool.submit(del);
        gate.countDown();
        int a = fa.get(30, TimeUnit.SECONDS), d = fd.get(30, TimeUnit.SECONDS);
        pool.shutdown();
        boolean deleted = postRows(p.getId()) == 0;
        if (deleted) { assertEquals(204, d); assertNotEquals(200, a); }
        else { assertEquals(200, a); assertEquals(409, d); assertEquals(1, participationRows(p.getId())); }
    }

    @Test
    void receivedInvitationsCarryTheMessage() throws Exception {
        var from = member("보낸사람");
        var to = member("받는사람");
        invitations.save(new InvitationEntity(null, from, to, "우리 밥 먹으러 갈래?", InvitationStatus.PENDING, LocalDateTime.now()));
        mvc.perform(get("/api/v1/invitations").with(as(to.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].message").value("우리 밥 먹으러 갈래?"))
                .andExpect(jsonPath("$.data[0].inviterName").value("보낸사람"));
    }

    @Test
    void articleDetailListsTaggedMembers() throws Exception {
        var author = member("작성자");
        var tagged = member("태그된친구");
        var a = articles.save(Article.builder().author(author).imageUrl("https://cdn.example.invalid/x.png")
                .mealDate(LocalDate.now()).mealTime(LocalTime.NOON).build());
        tags.save(ArticleTag.of(tagged, a));
        mvc.perform(get("/api/v1/articles/" + a.getId()).with(as(author.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taggedMemberIds[0]").value(tagged.getId()));
    }

    @Test
    void unknownPathIs404AndWrongMethodIs405() throws Exception {
        long me = member("x").getId();
        mvc.perform(get("/api/v1/no-such-endpoint").with(as(me)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("HTTP_404"));
        mvc.perform(get("/no-such-page").with(as(me))).andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/posts").with(as(me)))
                .andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.code").value("HTTP_405"));
        // 비로그인은 경로 확인 전에 보안 필터가 401을 준다(기존 동작 유지, 경로 존재 여부를 숨김).
        mvc.perform(get("/no-such-page")).andExpect(status().isUnauthorized());
    }
}
