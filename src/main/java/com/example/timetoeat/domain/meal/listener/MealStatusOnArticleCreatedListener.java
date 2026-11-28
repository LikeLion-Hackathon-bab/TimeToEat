package com.example.timetoeat.domain.meal.listener;

import com.example.timetoeat.domain.article.event.ArticleCreatedEvent;
import com.example.timetoeat.domain.article.repository.ArticleTagRepository;
import com.example.timetoeat.domain.meal.service.MealStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MealStatusOnArticleCreatedListener {

    private final MealStatusService mealStatusService;
    private final ArticleTagRepository tagRepository;

    // ArticleCommandService#createArticle() 에서 발행되는 이벤트를 AFTER_COMMIT에 처리
    // AFTER_COMMIT 시점의 REQUIRED 호출은 이미 커밋된 트랜잭션에 참여해 쓰기가 사라진다(F81).
    // 새 트랜잭션으로 글쓴이·태그된 멤버의 식사 상태를 한 번에 커밋한다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(ArticleCreatedEvent e) {
        // 1) 글쓴이 ON
        mealStatusService.markAte(e.getAuthorId(), e.getMealAtKst());

        // 2) 태그된 멤버도 ON
        List<Long> taggedIds = tagRepository.findTaggedMemberIdsByArticleId(e.getArticleId());
        taggedIds.forEach(id -> mealStatusService.markAte(id, e.getMealAtKst()));
    }
}
