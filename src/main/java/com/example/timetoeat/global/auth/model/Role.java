package com.example.timetoeat.global.auth.model;

import lombok.Getter;

@Getter
public enum Role {
    ROLE_MEMBER("ROLE_MEMBER", "일반회원"),
    ROLE_ADMIN("ROLE_ADMIN", "관리자");

    private final String key;      // Spring Security 인가에 사용되는 실제 권한명
    private final String description; // 프론트/로그 등 표시용

    Role(String key, String description) {
        this.key = key;
        this.description = description;
    }
}
