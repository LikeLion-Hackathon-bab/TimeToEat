package com.example.timetoeat.qa;

import com.example.timetoeat.domain.article.dto.ai.InferenceResultDto;
import com.example.timetoeat.domain.article.dto.request.CreateArticleRequest;
import com.example.timetoeat.domain.article.service.ArticleCommandService;
import com.example.timetoeat.domain.meal.repository.MemberMealStatusRepository;
import com.example.timetoeat.infra.ai.AiGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * F81: 게시물 작성 → 커밋 → AFTER_COMMIT 리스너(AI 추론 → meal_log, 식사 상태 → member_meal_status).
 * 리스너가 부르는 REQUIRED 서비스가 이미 커밋된 트랜잭션에 참여하면 쓰기가 커밋되지 않아 두 테이블이 0행이 된다.
 * 테스트 메서드는 트랜잭션 밖에서 실제 서비스를 불러 진짜 커밋과 AFTER_COMMIT 단계를 거친다. AI 서버만 대역.
 */
@TestPropertySource(properties = "app.ai.outbound-key=qa-ai-key")
class ArticleAfterCommitListenerIT extends QaIntegrationSupport {
    @Autowired ArticleCommandService articles;
    @Autowired MemberMealStatusRepository mealStatuses;
    @MockitoBean AiGateway ai;
    // 사진 내려받기는 https 공인 주소만 받으므로(ArticleImageFetcher) 시험에서는 임시 파일을 돌려주는 대역을 쓴다.
    @MockitoBean com.example.timetoeat.infra.ai.ArticleImageFetcher imageFetcher;

    @org.junit.jupiter.api.BeforeEach
    void fakeDownload() throws Exception {
        when(imageFetcher.fetch(anyString())).thenAnswer(i -> java.nio.file.Files.createTempFile("qa-img-", ".jpg"));
    }

    private CreateArticleRequest request(List<Long> tagged) {
        return CreateArticleRequest.builder()
                .imageUrl("https://cdn.example.com/articles/qa-sushi.jpg")
                .mealTime(LocalTime.of(12, 30))
                .taggedMemberIds(tagged)
                .method(com.example.timetoeat.domain.article.entity.UploadMethod.CAMERA)
                .build();
    }

    @Test
    void articleCreationWritesMealLogAndMealStatusForAuthorAndTagged() throws Exception {
        var author = member("작성자");
        var friend = member("태그친구");
        when(ai.inferFoodSync(anyLong(), anyLong(), anyString(), any()))
                .thenReturn(InferenceResultDto.of("01011001", "초밥", 0.93, OffsetDateTime.now(ZoneOffset.ofHours(9))));

        Long articleId = articles.createArticle(request(List.of(friend.getId())), author.getId());

        verify(ai).inferFoodSync(eq(articleId), eq(author.getId()), argThat(p -> p.endsWith(".jpg")), any());
        verify(imageFetcher).fetch("https://cdn.example.com/articles/qa-sushi.jpg");
        var logs = jdbc.queryForList("select member_id, label, code from meal_log where article_id=? order by member_id", articleId);
        assertEquals(2, logs.size(), "meal_log 행: " + logs);
        assertEquals(List.of(author.getId(), friend.getId()),
                logs.stream().map(r -> ((Number) r.get("member_id")).longValue()).toList());
        assertTrue(logs.stream().allMatch(r -> "초밥".equals(r.get("label")) && "01011001".equals(r.get("code"))));

        var expectedMealAt = LocalDateTime.of(LocalDate.now(java.time.ZoneId.of("Asia/Seoul")), LocalTime.of(12, 30));
        for (var m : List.of(author, friend)) {
            var rows = jdbc.queryForList("select last_meal_at, manual_fasting_since from member_meal_status where member_id=?", m.getId());
            assertEquals(1, rows.size(), "member_meal_status 행(member " + m.getId() + "): " + rows);
            assertNull(rows.get(0).get("manual_fasting_since"));
            // 시각 값은 앱 매핑으로 읽는다(JDBC 원시 읽기는 serverTimezone 변환이 섞인다)
            var saved = mealStatuses.findByMember_Id(m.getId()).orElseThrow();
            assertEquals(expectedMealAt, saved.getLastMealAt());
        }
    }

    /** AI가 실패해도 글과 식사 상태는 남고(리스너끼리 독립), meal_log만 비어 재시도 큐로 넘어간다. */
    @Test
    void aiFailureStillCommitsMealStatus() {
        var author = member("작성자2");
        when(ai.inferFoodSync(anyLong(), anyLong(), anyString(), any())).thenThrow(new IllegalStateException("ai down"));

        Long articleId = articles.createArticle(request(List.of()), author.getId());

        assertEquals(1, jdbc.queryForObject("select count(*) from article where id=?", Integer.class, articleId));
        assertEquals(0, jdbc.queryForObject("select count(*) from meal_log where article_id=?", Integer.class, articleId));
        assertEquals(1, jdbc.queryForObject("select count(*) from member_meal_status where member_id=? and last_meal_at is not null",
                Integer.class, author.getId()));
    }
}
