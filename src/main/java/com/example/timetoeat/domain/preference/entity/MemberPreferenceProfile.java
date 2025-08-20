package com.example.timetoeat.domain.preference.entity;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.global.util.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;

@Getter
@Entity
@Table(name = "member_preference_profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberPreferenceProfile extends BaseTimeEntity {

    @Id
    @Column(name = "member_id")
    private Long id;

    @OneToOne(fetch = LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "member_id")
    private MemberEntity member;

    @Column(nullable = false)
    private long revision = 0L;

    public static MemberPreferenceProfile create(MemberEntity member) {
        MemberPreferenceProfile p = new MemberPreferenceProfile();
        p.member = member;
        return p;
    }

    public void bumpRevision() {
        this.revision++;
    }
}
