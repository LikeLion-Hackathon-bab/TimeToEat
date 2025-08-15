package com.example.timetoeat.domain.posting.application.port.service.participation;

import com.example.timetoeat.domain.posting.application.port.in.usecase.participation.ParticipationUseCase;
import com.example.timetoeat.domain.posting.application.port.out.post.lock.PostLock;
import com.example.timetoeat.domain.posting.domain.vo.MemberId;
import com.example.timetoeat.domain.posting.domain.vo.PostId;
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
