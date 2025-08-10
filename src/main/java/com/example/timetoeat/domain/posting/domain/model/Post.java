package com.example.timetoeat.domain.posting.domain.model;

import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import com.example.timetoeat.domain.posting.domain.vo.PostId;
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
                message
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
            String message
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
                message
        );
    }

    public boolean isOwnedBy(MemberId memberId) {
        if (this.memberId == null || memberId == null) {
            return false;
        }
        return this.memberId.equals(memberId);
    }

    public boolean canApply(Participation participation) {
        return this.status == Status.OPEN && !participation.isFull(targetCount);
    }

    public Post close() {
        //상태 선검증 이러면 여러 요청이 들어와도 제대로 처리 가능
        if (this.status != Status.OPEN) {
            throw new IllegalStateException("이미 마감된 공고입니다.");
        }
        return Post.withId(
                this.postId,
                this.memberId,
                this.targetCount,
                Status.CLOSED, // 상태 변경
                this.createdAt,
                LocalDateTime.now(), // 만료 시간도 현재로 변경
                this.meetingAt,
                this.location,
                this.message
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
                Status.CLOSED, // 만료 시에도 상태는 CLOSED로 변경
                this.createdAt,
                this.expiredAt, // expiredAt 시간은 그대로 유지
                this.meetingAt,
                this.location,
                this.message
        );
    }
}
