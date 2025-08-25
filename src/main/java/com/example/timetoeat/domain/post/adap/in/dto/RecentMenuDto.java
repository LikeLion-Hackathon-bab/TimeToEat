package com.example.timetoeat.domain.post.adap.in.dto;

import java.util.List;

public record RecentMenuDto(
    String userId,
    List<MenuItemDto> menu
) {
}
