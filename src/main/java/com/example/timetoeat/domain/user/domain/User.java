package com.example.timetoeat.domain.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Where;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@DynamicInsert
@DynamicUpdate
@AllArgsConstructor
@Builder
@Where(clause = "is_deleted=false")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 선호 메뉴 (예: "한식, 중식, 샐러드", json, enum 등 선택 가능)
    @Column(nullable = true, length = 100)
    private String preferredMenus;

    // 비선호 메뉴 (동일 방식)
    @Column(nullable = true, length = 100)
    private String dislikedMenus;

    // 지금까지의 밥 약속 참여 횟수
    @Column(nullable = false)
    private int meetingCount;
}
