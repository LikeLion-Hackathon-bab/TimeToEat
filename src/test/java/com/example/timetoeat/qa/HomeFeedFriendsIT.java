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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * F09: 홈 피드는 내 글 + 친구 글 중 24시간 이내만, 최신순·페이징 유지.
 * 친구는 친구 목록 화면과 같은 출처(FriendDirectory: 추천 코드 수령 관계 우선 + 최근 가입자, 최대 30명)라
 * 목록에 보이는 사람 = 피드에 글이 보이는 사람.
 */
@AutoConfigureMockMvc
class HomeFeedFriendsIT extends QaIntegrationSupport {
    @Autowired MockMvc mvc;
    @Autowired ArticleRepository articles;
    @Autowired ReferralCodeRepository referrals;
    @Autowired FriendDirectory friendDirectory;
    @Autowired Clock clock; // 피드 기준 시각(KST)과 같은 시계
    private static final java.util.concurrent.atomic.AtomicInteger BATCH = new java.util.concurrent.atomic.AtomicInteger();

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

    /** DB의 다른 회원보다 늦게 가입한 회원 n명(가장 늦은 것이 마지막). 최근 가입자 30명 자리를 이들이 채운다. */
    private List<MemberEntity> newestMembers(int n, String prefix) {
        // 부를 때마다 더 늦은 시각대를 써서, 먼저 돈 시험의 회원이 끼어들지 않게 한다
        var base = LocalDateTime.now(clock).plusYears(1).plusDays(BATCH.incrementAndGet());
        List<MemberEntity> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            var m = member(prefix + i);
            setCreatedAt("member", m.getId(), base.plusMinutes(i));
            list.add(m);
        }
        return list;
    }

    @Test
    void feedShowsExactlyTheFriendListMembersPostsWithin24Hours() throws Exception {
        var me = member("나");
        var referralFriend = member("추천친구");   // 오래전 가입: 최근 가입 30명 밖이지만 추천 관계라 우선 포함
        var pendingInviter = member("미수령");     // 수령 안 된 코드는 친구 아님
        referral(referralFriend, me);             // 친구가 보낸 코드를 내가 수령
        referral(pendingInviter, null);
        var recent = newestMembers(30, "최근");    // recent.get(0) 이 30명 중 가장 오래됨
        var outsider = recent.get(0);             // 추천 친구 1 + 최근 29 = 30 이라 밀려남
        var recentFriend = recent.get(29);

        var friends = friendDirectory.friendIdsOf(me.getId());
        assertEquals(FriendDirectory.LIMIT, friends.size());
        assertEquals(referralFriend.getId(), friends.get(0), "추천 관계가 먼저");
        assertTrue(friends.contains(recentFriend.getId()));
        assertFalse(friends.contains(outsider.getId()));
        assertFalse(friends.contains(pendingInviter.getId()));

        var now = LocalDateTime.now(clock);
        long mine = post(me, now.minusMinutes(10));
        long f1 = post(referralFriend, now.minusMinutes(20));
        long f2 = post(recentFriend, now.minusHours(23));
        post(recentFriend, now.minusHours(25));    // 24시간 지남
        post(outsider, now.minusMinutes(5));       // 목록 밖
        post(pendingInviter, now.minusMinutes(5)); // 수락(수령) 전

        // 목록 화면과 피드가 같은 사람들
        mvc.perform(get("/api/v1/friends/me/meals").with(as(me.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(30))
                .andExpect(jsonPath("$.data[0].memberId").value(referralFriend.getId().intValue()))
                .andExpect(jsonPath("$.data[*].memberId", hasItem(recentFriend.getId().intValue())))
                .andExpect(jsonPath("$.data[*].memberId", not(hasItem(outsider.getId().intValue()))));

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

        // 추천 관계는 양방향: 추천 친구 쪽에서도 내 글이 보인다
        mvc.perform(get("/api/v1/articles/home").with(as(referralFriend.getId())))
                .andExpect(jsonPath("$.content[*].articleId", hasItem((int) mine)));
    }

    @Test
    void userWhoseFriendsHaveNoRecentPostsGetsAnEmptyFeedOfTheSameShape() throws Exception {
        var newbie = member("새회원");
        newestMembers(30, "조용한"); // 친구 30명 모두 글이 없음
        mvc.perform(get("/api/v1/articles/home").with(as(newbie.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
