package com.example.timetoeat.domain.post.domain;

import com.example.timetoeat.domain.participation.domain.Participation;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
public class Post {
    @Getter private final PostId postId;
    @Getter private final MemberId memberId;
    @Getter private final int targetCount;
    @Getter private final Status status;
    @Getter private final LocalDateTime createdAt;
    @Getter private final LocalDateTime expiredAt;
    @Getter private final LocalDateTime meetingAt;
    @Getter private final String location;
    @Getter private final String message;
    /** 마감 사유. OPEN이면 null. */
    @Getter private final CloseReason closeReason;

    public static Post withoutId(
            MemberId memberId,
            int targetCount,
            Status status,
            LocalDateTime createdAt,
            LocalDateTime expiredAt,
            LocalDateTime meetingAt,
            String location,
            String message
    ) {
        return new Post(
                null,
                memberId,
                targetCount,
                status,
                createdAt,
                expiredAt,
                meetingAt,
                location,
                message,
                null
        );
    }

    public static Post withId(
            PostId id,
            MemberId memberId,
            int targetCount,
            Status status,
            LocalDateTime createdAt,
            LocalDateTime expiredAt,
            LocalDateTime meetingAt,
            String location,
            String message,
            CloseReason closeReason
    ) {
        return new Post(
                id,
                memberId,
                targetCount,
                status,
                createdAt,
                expiredAt,
                meetingAt,
                location,
                message,
                closeReason
        );
    }

    public boolean isOwnedBy(MemberId memberId) {
        if (memberId == null) {
            throw new IllegalStateException("멤버 id가 비어있습니다");
        }
        return this.memberId.equals(memberId);
    }

    public boolean canApply(Participation participation) {
        return this.status == Status.OPEN && !participation.isFull(targetCount);
    }

    /** 작성자 수동 마감. */
    public Post close() {
        return close(CloseReason.MANUAL);
    }

    /** 수동 마감(MANUAL) 또는 인원 충족(FULL). expiredAt 은 기존대로 마감 시각으로 바꾼다(알림 중복 키 등이 의존). */
    public Post close(CloseReason reason) {
        //상태 선검증 이러면 여러 요청이 들어와도 제대로 처리 가능
        if (this.status != Status.OPEN) {
            throw new IllegalStateException("이미 마감된 공고입니다.");
        }
        return Post.withId(
                this.postId,
                this.memberId,
                this.targetCount,
                Status.CLOSED,
                this.createdAt,
                LocalDateTime.now(),
                this.meetingAt,
                this.location,
                this.message,
                reason
        );
    }

    public Post expire() {
        if (this.status != Status.OPEN) {
            return this;
        }
        return Post.withId(
                this.postId,
                this.memberId,
                this.targetCount,
                Status.CLOSED,
                this.createdAt,
                this.expiredAt,
                this.meetingAt,
                this.location,
                this.message,
                CloseReason.EXPIRED
        );
    }
}
