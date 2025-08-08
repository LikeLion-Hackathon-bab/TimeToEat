package com.example.timetoeat.domain.posting.application.port.service;

import com.example.timetoeat.domain.posting.application.port.in.usecase.ParticipationUseCase;
import com.example.timetoeat.domain.posting.application.port.out.PostLock;
import com.example.timetoeat.global.auth.entity.Member;
import com.example.timetoeat.domain.posting.domain.vo.PostId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ParticipationService implements ParticipationUseCase {

    private final PostLock postLock;
    private final ParticipationTransactionManager transactionManager;

    @Override
    public boolean apply(Member member, PostId postId) {
        postLock.lock(postId);
        try {
            transactionManager.apply(member, postId);
            return true;
        }finally {
            postLock.unlock(postId);
        }
    }
}
