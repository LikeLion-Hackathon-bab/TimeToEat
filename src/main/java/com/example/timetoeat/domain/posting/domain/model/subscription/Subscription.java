package com.example.timetoeat.domain.posting.domain.model.subscription;

import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import com.example.timetoeat.domain.posting.domain.vo.PostId;

public record Subscription(
        MemberId memberId,
        PostId postId
) {
    // 이것이 바로 '컴팩트 생성자'입니다.
    // record의 주 생성자가 호출되기 직전에 실행되어 유효성 검사를 담당합니다.
    public Subscription {
        if (memberId == null || postId == null) {
            throw new IllegalArgumentException("MemberId와 PostId는 필수입니다.");
        }
    }
}
