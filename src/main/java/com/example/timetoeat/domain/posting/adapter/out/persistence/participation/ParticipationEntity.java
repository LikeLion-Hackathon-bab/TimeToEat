package com.example.timetoeat.domain.posting.adapter.out.persistence.participation;

import com.example.timetoeat.domain.posting.adapter.out.persistence.post.PostEntity;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "participation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class ParticipationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    public ParticipationEntity(PostEntity post, MemberEntity member) {
        this.post = post;
        this.member = member;
    }
}
