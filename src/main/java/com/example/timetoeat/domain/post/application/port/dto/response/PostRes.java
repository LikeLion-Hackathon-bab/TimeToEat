package com.example.timetoeat.domain.post.application.port.dto.response;

import com.example.timetoeat.domain.post.application.port.dto.AuthorInfo;
import com.example.timetoeat.domain.post.application.port.dto.ParticipationInfo;

import java.time.LocalDateTime;
import java.util.List;

public record PostRes(
        Long postId,
        LocalDateTime createdAt,
        AuthorInfo author,
        String message,
        LocalDateTime meetingAt,
        String location,
        List<ParticipationInfo> participants,
        int targetCount,
        LocalDateTime expiredAt,
        /**
         * 추가 필드(검증 14 O1). 기존 필드는 그대로다.
         * status: OPEN | CLOSED | COMPLETED. 만료 시각이 지났는데 스케줄러(1분 주기)가 아직 마감하지 않은 공고도 CLOSED로 준다.
         * closeReason: OPEN이면 null, 아니면 MANUAL(작성자 마감, Client '마감') | FULL(인원 충족) | EXPIRED(기간 만료, Client '기간 만료').
         * close()가 expiredAt을 마감 시각으로 덮어쓰므로 Client는 expiredAt 대신 closeReason으로 구분한다.
         * 사유를 모르는 옛 공고(마이그레이션 전 CLOSED)는 null일 수 있다.
         */
        String status,
        String closeReason
){
}
