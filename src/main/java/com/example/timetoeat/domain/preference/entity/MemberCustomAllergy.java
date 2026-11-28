package com.example.timetoeat.domain.preference.entity;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;

/**
 * 온보딩·프로필 수정에서 사용자가 직접 입력한 알레르기(F07). food_code 선택지에 없는 자유 입력이라
 * member_food_preference(food_code FK)와 따로 둔다. 스키마: db/member_custom_allergy.sql
 */
@Getter
@Entity
@Table(name = "member_custom_allergy",
        indexes = @Index(name = "idx_mca_member", columnList = "member_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberCustomAllergy {

    public static final int MAX_COUNT = 10;
    public static final int MAX_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @Column(nullable = false, length = MAX_LENGTH)
    private String label;

    /** 입력 순서를 보존한다. */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public MemberCustomAllergy(MemberEntity member, String label, int sortOrder) {
        this.member = member;
        this.label = label;
        this.sortOrder = sortOrder;
    }
}
