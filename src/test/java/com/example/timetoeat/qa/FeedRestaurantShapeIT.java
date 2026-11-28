package com.example.timetoeat.qa;

import com.example.timetoeat.domain.article.entity.Article;
import com.example.timetoeat.domain.article.entity.RestaurantSnapshot;
import com.example.timetoeat.domain.article.repository.ArticleRepository;
import com.example.timetoeat.global.auth.dto.Oauth2UserInfo;
import com.example.timetoeat.global.auth.model.CustomOauth2User;
import com.example.timetoeat.global.auth.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** F19: 피드 요약 항목에 restaurant 객체(분류·주소·좌표)가 붙고, restaurantName 은 하위 호환으로 남는다. */
@AutoConfigureMockMvc
class FeedRestaurantShapeIT extends QaIntegrationSupport {
    @Autowired MockMvc mvc;
    @Autowired ArticleRepository articles;

    private RequestPostProcessor as(long memberId) {
        var user = new CustomOauth2User(Oauth2UserInfo.fromJwt(memberId, "qa", "qa@qa.test", Role.ROLE_MEMBER));
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void feedSummaryCarriesTheRestaurantObject() throws Exception {
        var author = member("작성자");
        articles.save(Article.builder().author(author).imageUrl("https://cdn.example.invalid/a.png")
                .mealDate(LocalDate.now()).mealTime(LocalTime.NOON).build());
        articles.save(Article.builder().author(author).imageUrl("https://cdn.example.invalid/b.png")
                .mealDate(LocalDate.now()).mealTime(LocalTime.NOON)
                .restaurant(RestaurantSnapshot.builder().placeId("1000001").placeName("검증 식당")
                        .addressName("검증동 1").roadAddressName("검증로 1").categoryGroupCode("FD6")
                        .categoryGroupName("음식점").categoryName("음식점 > 한식").x(127.0).y(37.0).build())
                .build());
        mvc.perform(get("/api/v1/articles/by-author/" + author.getId()).with(as(author.getId())))
                .andExpect(status().isOk())
                // 최신순: 식당 있는 글이 먼저
                .andExpect(jsonPath("$.content[0].restaurantName").value("검증 식당"))
                .andExpect(jsonPath("$.content[0].restaurant.placeId").value("1000001"))
                .andExpect(jsonPath("$.content[0].restaurant.placeName").value("검증 식당"))
                .andExpect(jsonPath("$.content[0].restaurant.categoryName").value("음식점 > 한식"))
                .andExpect(jsonPath("$.content[0].restaurant.categoryGroupName").value("음식점"))
                .andExpect(jsonPath("$.content[0].restaurant.addressName").value("검증동 1"))
                .andExpect(jsonPath("$.content[0].restaurant.roadAddressName").value("검증로 1"))
                .andExpect(jsonPath("$.content[0].restaurant.x").value(127.0))
                .andExpect(jsonPath("$.content[0].restaurant.y").value(37.0))
                // 식당 없는 글은 restaurant 가 null 이고 restaurantName 도 null
                .andExpect(jsonPath("$.content[1].restaurant").doesNotExist())
                .andExpect(jsonPath("$.content[1].restaurantName").doesNotExist());
    }
}
