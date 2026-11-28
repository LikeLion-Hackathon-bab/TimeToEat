package com.example.timetoeat.domain.participation.application.service;

import com.example.timetoeat.domain.post.application.port.out.PostLock;
import com.example.timetoeat.domain.participation.application.port.in.usecase.ParticipationUseCase;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import com.example.timetoeat.global.error.GlobalErrorCode;
import com.example.timetoeat.global.error.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ParticipationService implements ParticipationUseCase {

    private final PostLock postLock;
    private final ParticipationTransactionManager transactionManager;

    @Override
    public boolean apply(MemberId memberId, PostId postId) {
        // 회원·공고 id가 될 수 없는 값(0 이하, 가짜 시연 친구 id)은 400. 잠금·DB 접근 전에 거른다.
        if (memberId == null || memberId.getId() == null || memberId.getId() <= 0
                || postId == null || postId.getId() == null || postId.getId() <= 0) {
            throw new CustomException(GlobalErrorCode.BAD_REQUEST);
        }
        postLock.lock(postId);
        try {
            if (!transactionManager.apply(memberId, postId)) throw new ResponseStatusException(HttpStatus.GONE, "만료된 공고입니다.");
            return true;
        }finally {
            postLock.unlock(postId);
        }
    }
}
