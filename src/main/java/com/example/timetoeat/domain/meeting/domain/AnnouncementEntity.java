package com.example.timetoeat.domain.meeting.domain;

import com.example.timetoeat.domain.user.domain.User;
import com.example.timetoeat.global.util.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@DynamicInsert
@DynamicUpdate
@AllArgsConstructor
@Builder
public class AnnouncementEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 주최자: User 엔티티와 ManyToOne 관계, 지연 로딩 권장
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_user_id", nullable = false)
    private User hostUser;

    // 날짜: 오늘로 고정이므로 LocalDate (날짜만 필요)
    @Column(nullable = false)
    private LocalDate date;

    // 시간: 사용자가 선택, LocalTime 사용
    private LocalTime time;

    // 장소 (필수)
    @Column(nullable = false)
    private String location;

    // 파티원 인원 수 제한, 0일 경우 무제한
    @Column(nullable = false)
    private int memberLimit;

    // 만료시간 (등록 시간 + 2시간) - 보통 LocalDateTime 사용
    @Column(nullable = false)
    private LocalDateTime expireAt;

    // 상태 (모집중, 마감, 완료) - Enum 타입으로 정의하는 게 좋음
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnnouncementStatus status;

}
