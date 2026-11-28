package com.example.timetoeat.domain.post.domain;

/**
 * 공고가 OPEN에서 벗어난(마감된) 이유. post.close_reason 에 저장한다(db/post_close_reason.sql).
 * Post.close() 가 expiredAt 을 마감 시각으로 덮어써 Client 가 수동 마감과 기간 만료를 구분할 수 없었다(검증 14 O1).
 * OPEN 공고는 null 이다. 다시 모집(RoomLeaveService)으로 OPEN 이 되면 지운다.
 */
public enum CloseReason {
    /** 작성자가 직접 마감(POST /api/v1/posts/{id}/close). Client 라벨 '마감'. */
    MANUAL,
    /** 모집 인원이 다 차서 자동 마감. */
    FULL,
    /** 모집 기간(expiredAt)이 지나 마감(스케줄러 또는 참여 시 410). Client 라벨 '기간 만료'. */
    EXPIRED
}
