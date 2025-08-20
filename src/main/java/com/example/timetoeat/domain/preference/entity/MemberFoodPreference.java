package com.example.timetoeat.domain.preference.entity;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;

@Getter
@Entity
@Table(
        name = "member_food_preference",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_food_type",
                columnNames = {"member_id", "food_code", "type"}
        ),
        indexes = {
                @Index(name = "idx_mfp_member_type", columnList = "member_id, type")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberFoodPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "food_code", referencedColumnName = "code", nullable = false)
    private FoodCode foodCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PreferenceType type;

    @Builder
    private MemberFoodPreference(MemberEntity member, FoodCode foodCode, PreferenceType type) {
        this.member = member;
        this.foodCode = foodCode;
        this.type = type;
    }
}
