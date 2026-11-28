package com.example.timetoeat.qa;

import com.example.timetoeat.domain.article.entity.Article;
import com.example.timetoeat.domain.article.repository.ArticleRepository;
import com.example.timetoeat.domain.friend.service.FriendDirectory;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.referral.entity.ReferralCode;
import com.example.timetoeat.domain.referral.repository.ReferralCodeRepository;
import com.example.timetoeat.global.auth.dto.Oauth2UserInfo;
import com.example.timetoeat.global.auth.model.CustomOauth2User;
import com.example.timetoeat.global.auth.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F09: 홈 피드는 내 글 + 친구 글 중 24시간 이내만, 최신순·페이징 유지.
 * 친구는 친구 목록 화면과 같은 출처(FriendDirectory: 추천 코드 수령 관계만, 최대 30명. 모르는 사람으로 채우지 않음)라
 * 목록에 보이는 사람 = 피드에 글이 보이는 사람.
 */
@AutoConfigureMockMvc
class HomeFeedFriendsIT extends QaIntegrationSupport {
    @Autowired MockMvc mvc;
    @Autowired ArticleRepository articles;
    @Autowired ReferralCodeRepository referrals;
    @Autowired FriendDirectory friendDirectory;
    @Autowired Clock clock; // 피드 기준 시각(KST)과 같은 시계

    private RequestPostProcessor as(long memberId) {
        var user = new CustomOauth2User(Oauth2UserInfo.fromJwt(memberId, "qa", "qa@qa.test", Role.ROLE_MEMBER));
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    // Timestamp 로 넘겨 Hibernate 가 피드 기준 시각을 바인딩할 때와 같은 시간대 변환을 타게 한다
    private void setCreatedAt(String table, long id, LocalDateTime at) {
        jdbc.update("update " + table + " set created_at=? where id=?", Timestamp.valueOf(at), id);
    }

    private long post(MemberEntity author, LocalDateTime createdAt) {
        var a = articles.save(Article.builder().author(author).imageUrl("https://cdn.example.invalid/" + UUID.randomUUID() + ".png")
                .mealDate(LocalDate.now()).mealTime(LocalTime.NOON).build());
        setCreatedAt("article", a.getId(), createdAt);
        return a.getId();
    }

    private void referral(MemberEntity inviter, MemberEntity receiver) {
        var r = ReferralCode.builder().inviter(inviter).code(UUID.randomUUID().toString().substring(0, 10))
                .expiresAt(LocalDateTime.now().plusDays(7)).build();
        if (receiver != null) r.markUsed(receiver, LocalDateTime.now());
        referrals.save(r);
    }

    @Test
    void feedShowsOnlyMeAndReferralFriendsWithin24Hours() throws Exception {
        var me = member("나");
        var friendA = member("김민준");          // 친구가 보낸 코드를 내가 수령
        var friendB = member("최서연");          // 내가 보낸 코드를 친구가 수령
        var pendingInviter = member("미수령");   // 수령 안 된 코드는 친구 아님
        var stranger = member("모르는사람");     // 방금 가입했지만 관계 없음
        referral(friendA, me);
        referral(me, friendB);
        referral(pendingInviter, null);

        var friends = friendDirectory.friendIdsOf(me.getId());
        assertEquals(List.of(friendB.getId(), friendA.getId()).stream().sorted().toList(), friends.stream().sorted().toList());

        var now = LocalDateTime.now(clock);
        long mine = post(me, now.minusMinutes(10));
        long f1 = post(friendA, now.minusMinutes(20));
        long f2 = post(friendB, now.minusHours(23));
        post(friendB, now.minusHours(25));        // 24시간 지남
        post(stranger, now.minusMinutes(5));      // 친구 아님
        post(pendingInviter, now.minusMinutes(5)); // 수락(수령) 전

        // 목록 화면과 피드가 같은 사람들
        mvc.perform(get("/api/v1/friends/me/meals").with(as(me.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[*].memberId", containsInAnyOrder(friendA.getId().intValue(), friendB.getId().intValue())));

        mvc.perform(get("/api/v1/articles/home").with(as(me.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].articleId").value(contains((int) mine, (int) f1, (int) f2)));

        // 페이징·정렬 유지
        mvc.perform(get("/api/v1/articles/home?page=1&size=2").with(as(me.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].articleId").value((int) f2));

        // 추천 관계는 양방향: 친구 쪽에서도 내 글이 보인다
        mvc.perform(get("/api/v1/articles/home").with(as(friendA.getId())))
                .andExpect(jsonPath("$.content[*].articleId", hasItem((int) mine)));
        // 모르는 사람 쪽에서는 내 글이 안 보인다
        mvc.perform(get("/api/v1/articles/home").with(as(stranger.getId())))
                .andExpect(jsonPath("$.content[*].articleId", not(hasItem((int) mine))));
    }

    @Test
    void memberWithNoFriendsGetsEmptyFriendListAndOnlyOwnPosts() throws Exception {
        var newbie = member("새회원");
        var other = member("다른회원");
        post(other, LocalDateTime.now(clock).minusMinutes(1)); // 방금 올린 남의 글
        mvc.perform(get("/api/v1/friends/me/meals").with(as(newbie.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
        mvc.perform(get("/api/v1/articles/home").with(as(newbie.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
