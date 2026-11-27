package com.example.timetoeat.domain.plans.meeting;

import jakarta.persistence.*;
import lombok.Getter;
import com.example.timetoeat.domain.post.adap.out.persistence.PostEntity;

@Entity
@Getter
public class ConfirmedMeeting {
    @Id private Long postId;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private PostEntity post;
    @Column(nullable = false) private String restaurant;
}
