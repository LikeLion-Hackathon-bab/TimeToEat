package com.example.timetoeat.qa;

import com.example.timetoeat.domain.invitation.adap.out.persistence.InvitationEntity;
import com.example.timetoeat.domain.invitation.adap.out.persistence.InvitationRepository;
import com.example.timetoeat.domain.invitation.domain.InvitationStatus;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.notification.service.NotificationTriggerRecorder;
import com.example.timetoeat.domain.participation.adap.out.persistence.ParticipationEntity;
import com.example.timetoeat.domain.participation.adap.out.persistence.ParticipationRepository;
import com.example.timetoeat.domain.post.domain.Status;
import com.example.timetoeat.global.auth.dto.Oauth2UserInfo;
import com.example.timetoeat.global.auth.model.CustomOauth2User;
import com.example.timetoeat.global.auth.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F25 알림 종류(기획 문서 §14.2, 계약 qa-cloud/contracts/notification-types.md).
 * 각 종류를 실제 API로 일으키고(커밋 → AFTER_COMMIT → 알림 스레드 저장) 수신자·문구·url·중복 방지를 본다.
 * 알림 저장은 비동기라 최대 5초 기다린다.
 */
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "QA_IT", matches = "1")
@TestPropertySource(properties = "notification.reminder.scheduler.enabled=false")
class NotificationTypesIT extends QaIntegrationSupport {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Autowired MockMvc mvc;
    @Autowired ParticipationRepository participations;
    @Autowired InvitationRepository invitations;
    @Autowired NotificationTriggerRecorder recorder;

    private RequestPostProcessor as(long memberId) {
        var user = new CustomOauth2User(Oauth2UserInfo.fromJwt(memberId, "qa", "qa@qa.test", Role.ROLE_MEMBER));
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    /** 수신자의 해당 종류 알림이 count 개 될 때까지 기다린 뒤 최신순으로 돌려준다. */
    private List<Map<String, Object>> await(long receiver, String type, int count) throws InterruptedException {
        List<Map<String, Object>> rows = List.of();
        for (int i = 0; i < 50; i++) {
            rows = rows(receiver, type);
            if (rows.size() >= count) break;
            Thread.sleep(100);
        }
        Thread.sleep(300); // 넘치게 만들지 않는지(중복) 보려고 조금 더 기다린 뒤 다시 센다
        rows = rows(receiver, type);
        assertEquals(count, rows.size(), type + " for " + receiver + ": " + rows);
        return rows;
    }

    private List<Map<String, Object>> rows(long receiver, String type) {
        return jdbc.queryForList("select category, title, body, url, is_read from notification where receiver_id=? and type=? order by id desc",
                receiver, type);
    }

    private void none(long receiver, String type) throws InterruptedException {
        Thread.sleep(500);
        assertEquals(0, rows(receiver, type).size(), type + " must not go to " + receiver);
    }

    private long article(MemberEntity author, List<Long> tagged) throws Exception {
        String tags = tagged.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
        mvc.perform(post("/api/v1/articles").with(as(author.getId())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"imageUrl\":\"uploads/qa.jpg\",\"mealTime\":\"12:30:00\",\"method\":\"CAMERA\",\"taggedMemberIds\":[" + tags + "]}"))
                .andExpect(status().is2xxSuccessful());
        return jdbc.queryForObject("select max(id) from article where author_id=?", Long.class, author.getId());
    }

    // 1. POST_TAGGED
    @Test
    void taggedFriendGetsPostTagged() throws Exception {
        var author = member("김민준"); var friend = member("최서연");
        long id = article(author, List.of(friend.getId(), author.getId()));
        var n = await(friend.getId(), "POST_TAGGED", 1).get(0);
        assertEquals("ALARM", n.get("category"));
        assertEquals("식사 인증에 태그됐어요", n.get("title"));
        assertEquals("김민준님이 함께한 식사를 올렸어요.", n.get("body"));
        assertEquals("/post/" + id, n.get("url"));
        none(author.getId(), "POST_TAGGED");
    }

    // 2. POST_LIKED: 켜기만, 껐다 켜도 1번, 자기 글은 없음
    @Test
    void likeNotifiesAuthorOnceAndNotSelf() throws Exception {
        var author = member("박지윤"); var fan = member("강태민");
        long id = article(author, List.of());
        mvc.perform(post("/api/v1/articles/" + id + "/like").with(as(fan.getId()))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/articles/" + id + "/like").with(as(fan.getId()))).andExpect(status().isOk()); // 취소
        mvc.perform(post("/api/v1/articles/" + id + "/like").with(as(fan.getId()))).andExpect(status().isOk()); // 다시
        mvc.perform(post("/api/v1/articles/" + id + "/like").with(as(author.getId()))).andExpect(status().isOk());
        var n = await(author.getId(), "POST_LIKED", 1).get(0);
        assertEquals("좋아요를 받았어요", n.get("title"));
        assertEquals("강태민님이 내 식사 인증을 좋아해요.", n.get("body"));
        assertEquals("/post/" + id, n.get("url"));
    }

    private long comment(long articleId, MemberEntity who, Long parent, String text) throws Exception {
        String p = parent == null ? "" : "\"parentCommentId\":" + parent + ",";
        mvc.perform(post("/api/v1/articles/" + articleId + "/comments").with(as(who.getId())).contentType(MediaType.APPLICATION_JSON)
                .content("{" + p + "\"content\":\"" + text + "\"}")).andExpect(status().is2xxSuccessful());
        return jdbc.queryForObject("select max(id) from article_comment where author_id=?", Long.class, who.getId());
    }

    // 3. COMMENT_CREATED
    @Test
    void commentNotifiesArticleAuthorWithPreviewAndCommentLink() throws Exception {
        var author = member("이소율"); var c = member("정하준");
        long id = article(author, List.of());
        String text = "맛있어 보여요! 다음엔 저도 같이 가요 꼭이요 진짜로 정말로 약속해요 네네네";
        long cid = comment(id, c, null, text);
        comment(id, author, null, "내 댓글");
        var n = await(author.getId(), "COMMENT_CREATED", 1).get(0);
        assertEquals("새 댓글이 달렸어요", n.get("title"));
        assertEquals("정하준님: " + text.substring(0, 40) + "…", n.get("body"));
        assertEquals("/post/" + id + "?comment=" + cid, n.get("url"));
    }

    // 4. REPLY_CREATED: 부모 댓글 작성자에게. 글 작성자가 부모면 REPLY 하나만.
    @Test
    void replyNotifiesParentAuthorAndArticleAuthorOnce() throws Exception {
        var author = member("윤서아"); var c = member("임도윤"); var r = member("한지민");
        long id = article(author, List.of());
        long parent = comment(id, c, null, "부모");
        long reply = comment(id, r, parent, "답글이에요");
        var n = await(c.getId(), "REPLY_CREATED", 1).get(0);
        assertEquals("내 댓글에 답글이 달렸어요", n.get("title"));
        assertEquals("한지민님: 답글이에요", n.get("body"));
        assertEquals("/post/" + id + "?comment=" + reply, n.get("url"));
        await(author.getId(), "COMMENT_CREATED", 2); // 부모 댓글 + 답글

        long own = comment(id, author, null, "글쓴이 댓글");
        comment(id, c, own, "글쓴이에게 답글");
        await(author.getId(), "REPLY_CREATED", 1);
        await(author.getId(), "COMMENT_CREATED", 2); // 같은 답글로 COMMENT_CREATED 는 안 늘어남
    }

    // 5. ANNOUNCEMENT_JOINED
    @Test
    void joiningAnnouncementNotifiesOwner() throws Exception {
        var owner = member("오시우"); var joiner = member("서유나");
        var p = openPost(owner, 3);
        mvc.perform(post("/api/v1/participate/" + p.getId()).with(as(joiner.getId()))).andExpect(status().isOk());
        var n = await(owner.getId(), "ANNOUNCEMENT_JOINED", 1).get(0);
        assertEquals("내 공고에 새 참여자가 있어요", n.get("title"));
        assertEquals("서유나님이 강남역 공고에 참여했어요. (1/3명)", n.get("body"));
        assertEquals("/matching?tab=announcement", n.get("url"));
    }

    @Test
    void rejectedJoinMakesNoNotification() throws Exception {
        var owner = member("남궁민"); var joiner = member("황보라");
        var p = savePost(owner, 1, Status.CLOSED, LocalDateTime.now().plusHours(1), LocalDateTime.now().plusDays(1));
        mvc.perform(post("/api/v1/participate/" + p.getId()).with(as(joiner.getId()))).andExpect(status().isConflict());
        none(owner.getId(), "ANNOUNCEMENT_JOINED");
    }

    // 9. MEETING_COORDINATING: 공고 대기실, 초대 대기실
    @Test
    void enteringRoomsNotifiesTheOthersOncePerRound() throws Exception {
        var owner = member("조은우"); var friend = member("문가은");
        var p = savePost(owner, 1, Status.CLOSED, LocalDateTime.now().plusHours(1), LocalDateTime.now(SEOUL).plusDays(1));
        participations.save(new ParticipationEntity(p, friend));
        mvc.perform(post("/api/v1/meetings/rooms/" + p.getId() + "/enter").with(as(friend.getId()))).andExpect(status().is2xxSuccessful());
        mvc.perform(post("/api/v1/meetings/rooms/" + p.getId() + "/enter").with(as(friend.getId()))).andExpect(status().is2xxSuccessful());
        var n = await(owner.getId(), "MEETING_COORDINATING", 1).get(0);
        assertEquals("약속 조율이 시작됐어요", n.get("title"));
        assertEquals("문가은님이 강남역 밥약 대기실에 들어왔어요.", n.get("body"));
        assertEquals("/announcement/waiting/" + p.getId(), n.get("url"));
        none(friend.getId(), "MEETING_COORDINATING");

        var inviter = member("배서진"); var invitee = member("신예준");
        long inv = invitations.save(new InvitationEntity(null, inviter, invitee, "밥", InvitationStatus.APPROVED, LocalDateTime.now())).getId();
        mvc.perform(post("/api/v1/invitations/" + inv + "/enter").with(as(invitee.getId()))).andExpect(status().isNoContent());
        var m = await(inviter.getId(), "MEETING_COORDINATING", 1).get(0);
        assertEquals("신예준님이 초대 대기실에 들어왔어요.", m.get("body"));
        assertEquals("/invitation/waiting/" + inv, m.get("url"));
    }

    // 10. MEETING_CONFIRMED (공고·초대), 재전송 1회
    @Test
    void confirmedMeetingNotifiesEveryParticipantOnce() throws Exception {
        var owner = member("권나연"); var friend = member("유건우");
        var meetingAt = LocalDateTime.of(2026, 12, 3, 12, 30);
        var p = savePost(owner, 1, Status.CLOSED, LocalDateTime.now().plusHours(1), meetingAt);
        participations.save(new ParticipationEntity(p, friend));
        for (int i = 0; i < 2; i++)
            mvc.perform(post("/api/v1/meetings/" + p.getId() + "/result").header("X-Room-Key", "qa-room-key")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"restaurant\":\"초밥집\"}")).andExpect(status().isNoContent());
        for (var who : List.of(owner, friend)) {
            var n = await(who.getId(), "MEETING_CONFIRMED", 1).get(0);
            assertEquals("식사 약속이 확정됐어요", n.get("title"));
            assertEquals("12월 3일 12:30 초밥집에서 만나요.", n.get("body"));
            assertEquals("/meeting?type=ANNOUNCEMENT&id=" + p.getId(), n.get("url"));
        }
        // 다른 식당으로 409 → 알림 늘지 않음
        mvc.perform(post("/api/v1/meetings/" + p.getId() + "/result").header("X-Room-Key", "qa-room-key")
                .contentType(MediaType.APPLICATION_JSON).content("{\"restaurant\":\"국밥집\"}")).andExpect(status().isConflict());
        await(owner.getId(), "MEETING_CONFIRMED", 1);

        var inviter = member("송하린"); var invitee = member("노지호");
        long inv = invitations.save(new InvitationEntity(null, inviter, invitee, "밥", InvitationStatus.APPROVED, LocalDateTime.now())).getId();
        mvc.perform(post("/api/v1/meetings/invitations/" + inv + "/result").header("X-Room-Key", "qa-room-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"meetingAt\":\"2026-12-04T13:00:00+09:00\",\"location\":\"신촌\",\"restaurant\":\"국수집\"}"))
                .andExpect(status().isNoContent());
        var n = await(invitee.getId(), "MEETING_CONFIRMED", 1).get(0);
        assertEquals("12월 4일 13:00 국수집에서 만나요.", n.get("body"));
        assertEquals("/meeting?type=INVITATION&id=" + inv, n.get("url"));
        await(inviter.getId(), "MEETING_CONFIRMED", 1);
    }

    // 11. MEETING_REMINDER: 30분 안쪽만, 두 번 돌아도 1회
    @Test
    void reminderGoesOnceForMeetingsWithin30Minutes() throws Exception {
        var now = LocalDateTime.now(SEOUL).withNano(0);
        var owner = member("고은서"); var friend = member("홍시윤");
        var soon = savePost(owner, 1, Status.CLOSED, now.minusHours(1), now.plusMinutes(20));
        participations.save(new ParticipationEntity(soon, friend));
        jdbc.update("insert into confirmed_meeting(post_id, restaurant) values (?, '덮밥집')", soon.getId());
        var later = savePost(owner, 1, Status.CLOSED, now.minusHours(1), now.plusMinutes(90));
        jdbc.update("insert into confirmed_meeting(post_id, restaurant) values (?, '피자집')", later.getId());

        recorder.remindUpcoming();
        recorder.remindUpcoming();
        for (var who : List.of(owner, friend)) {
            var n = await(who.getId(), "MEETING_REMINDER", 1).get(0);
            assertEquals("곧 식사 약속이 있어요", n.get("title"));
            assertEquals(now.plusMinutes(20).format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) + " 덮밥집 약속이 곧 시작돼요.", n.get("body"));
            assertEquals("/meeting?type=ANNOUNCEMENT&id=" + soon.getId(), n.get("url"));
        }
    }

    // 12. REFERRAL_ACTIVITY + 14. COUPON_ISSUED (추천 코드 수령 보상)
    @Test
    void redeemingReferralNotifiesInviterAndGivesRedeemerCouponNotice() throws Exception {
        var inviter = member("장민서"); var newbie = member("전우진");
        var body = mvc.perform(post("/api/v1/referrals").with(as(inviter.getId()))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String code = com.jayway.jsonpath.JsonPath.read(body, "$.data.code");
        mvc.perform(post("/api/v1/referrals/redeem").param("code", code).with(as(newbie.getId()))).andExpect(status().isNoContent());

        var r = await(inviter.getId(), "REFERRAL_ACTIVITY", 1).get(0);
        assertEquals("추천한 친구가 밥먹댕에 왔어요", r.get("title"));
        assertEquals("전우진님이 내 추천 코드로 가입했어요.", r.get("body"));
        assertEquals("/profile", r.get("url"));

        var c = await(newbie.getId(), "COUPON_ISSUED", 1).get(0);
        assertEquals("쿠폰이 도착했어요", c.get("title"));
        assertEquals("친구 초대 리워드 쿠폰이 쿠폰함에 들어왔어요.", c.get("body"));
        assertEquals("/coupon", c.get("url"));

        // 이미 쓴 코드 → 409 계열 실패, 알림 늘지 않음
        var again = member("다른사람");
        mvc.perform(post("/api/v1/referrals/redeem").param("code", code).with(as(again.getId()))).andExpect(status().is4xxClientError());
        none(again.getId(), "COUPON_ISSUED");
        await(inviter.getId(), "REFERRAL_ACTIVITY", 1);
    }

    // 13. CHALLENGE_ACHIEVED (+ 보상 수령 시 14. COUPON_ISSUED)
    @Test
    void seventhDayInARowNotifiesChallengeAndRewardCouponNotifies() throws Exception {
        var me = member("류지아");
        LocalDate today = LocalDate.now(SEOUL);
        for (int d = 1; d <= 6; d++) {
            article(me, List.of());
            long id = jdbc.queryForObject("select max(id) from article where author_id=?", Long.class, me.getId());
            jdbc.update("update article set created_at=? where id=?", today.minusDays(d).atTime(12, 0), id);
        }
        // 위 6개는 만든 날이 오늘이라 오늘 연속 1~6일째로 보였을 뿐 7의 배수가 아니다 → 아직 알림 없음
        none(me.getId(), "CHALLENGE_ACHIEVED");
        article(me, List.of()); // 오늘: 7일 연속
        article(me, List.of()); // 같은 날 두 번째 글: 중복 알림 없음
        var n = await(me.getId(), "CHALLENGE_ACHIEVED", 1).get(0);
        assertEquals("주간 챌린지를 달성했어요", n.get("title"));
        assertEquals("7일 연속 식사 인증 완료! 보상 쿠폰을 받아 보세요.", n.get("body"));
        assertEquals("/challenge", n.get("url"));

        mvc.perform(post("/api/v1/challenges/me/reward").param("type", "WEEK").with(as(me.getId()))).andExpect(status().isNoContent());
        var c = await(me.getId(), "COUPON_ISSUED", 1).get(0);
        assertEquals("주간 챌린지 리워드 쿠폰이 쿠폰함에 들어왔어요.", c.get("body"));
    }

    // read-all: 내 ALARM만
    @Test
    void readAllMarksOnlyMyAlarms() throws Exception {
        var author = member("백승현"); var fan = member("안채원"); var other = member("양태오");
        long id = article(author, List.of());
        mvc.perform(post("/api/v1/articles/" + id + "/like").with(as(fan.getId()))).andExpect(status().isOk());
        comment(id, fan, null, "좋아요");
        await(author.getId(), "COMMENT_CREATED", 1);
        long otherArticle = article(other, List.of());
        mvc.perform(post("/api/v1/articles/" + otherArticle + "/like").with(as(fan.getId()))).andExpect(status().isOk());
        await(other.getId(), "POST_LIKED", 1);

        mvc.perform(get("/api/v1/notifications/unread-count").with(as(author.getId()))).andExpect(jsonPath("$.data.unread").value(2));
        mvc.perform(patch("/api/v1/notifications/read-all").with(as(author.getId()))).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/notifications/unread-count").with(as(author.getId()))).andExpect(jsonPath("$.data.unread").value(0));
        mvc.perform(get("/api/v1/notifications/unread-count").with(as(other.getId()))).andExpect(jsonPath("$.data.unread").value(1));
        mvc.perform(get("/api/v1/notifications").with(as(author.getId())))
                .andExpect(jsonPath("$.data[0].type").value("COMMENT_CREATED"))
                .andExpect(jsonPath("$.data[0].read").value(true))
                .andExpect(jsonPath("$.data[1].type").value("POST_LIKED"));
        mvc.perform(patch("/api/v1/notifications/read-all")).andExpect(status().isUnauthorized());
    }
}
