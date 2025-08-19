package com.example.timetoeat.domain.posting.core.application.participation;

import com.example.timetoeat.domain.posting.core.application.port.out.gateway.post.lock.PostLock;
import com.example.timetoeat.domain.posting.core.application.port.in.usecase.usecase.participation.ParticipationUseCase;
import com.example.timetoeat.domain.posting.core.domain.vo.member.MemberId;
import com.example.timetoeat.domain.posting.core.domain.vo.post.PostId;
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
            transactionManager.apply(memberId, postId);
            return true;
        }finally {
            postLock.unlock(postId);
        }
    }
}
