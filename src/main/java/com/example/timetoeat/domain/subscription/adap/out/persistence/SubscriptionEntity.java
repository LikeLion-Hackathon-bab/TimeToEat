package com.example.timetoeat.domain.subscription.adap.out.persistence;

import com.example.timetoeat.domain.post.adap.out.persistence.PostEntity;
import com.example.timetoeat.domain.member.entity.MemberEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscription",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "member_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class SubscriptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private MemberEntity member;

    public SubscriptionEntity(PostEntity post, MemberEntity member) {
        this.post = post;
        this.member = member;
    }
}
