package com.example.timetoeat.domain.participation.application.service;

import com.example.timetoeat.domain.post.application.port.out.PostLock;
import com.example.timetoeat.domain.participation.application.port.in.usecase.ParticipationUseCase;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
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
        postLock.lock(postId);
        try {
            if (!transactionManager.apply(memberId, postId)) throw new ResponseStatusException(HttpStatus.GONE, "만료된 공고입니다.");
            return true;
        }finally {
            postLock.unlock(postId);
        }
    }
}
