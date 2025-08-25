package com.example.timetoeat.domain.post.application.service;

import com.example.timetoeat.domain.article.dto.response.RecentMealsResponse;
import com.example.timetoeat.domain.article.service.ArticleMealQueryService;
import com.example.timetoeat.domain.post.adap.in.dto.MenuItemDto;
import com.example.timetoeat.domain.post.adap.in.dto.RecentMenuDto;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RecentMenuService {
    private final ArticleMealQueryService articleMealQueryService;

    public List<RecentMenuDto> getRecentMenus(Set<MemberId> memberIds) {
        return memberIds.stream()
                .map(memberId -> {
                    RecentMealsResponse mealsResponse = articleMealQueryService.getMyRecentMeals(memberId.getId());

                    List<MenuItemDto> menuItems = mealsResponse.getItems().stream()
                            .map(item -> new MenuItemDto(
                                    item.getCode(),
                                    item.getLabel()
                            ))
                            .toList();

                    return new RecentMenuDto(
                            String.valueOf(memberId.getId()),
                            menuItems
                    );
                })
                .toList();
    }
}
