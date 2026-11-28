package com.example.timetoeat.qa;

import com.example.timetoeat.domain.post.application.port.in.command.CreatePostCommand;
import com.example.timetoeat.domain.post.application.port.in.usecase.GetPostUseCase;
import com.example.timetoeat.domain.post.application.port.in.usecase.PostUseCase;
import com.example.timetoeat.domain.post.application.port.dto.response.PostRes;
import com.example.timetoeat.domain.post.domain.Status;
import com.example.timetoeat.domain.post.domain.vo.member.MemberId;
import com.example.timetoeat.domain.post.domain.vo.post.PostId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** GET /api/v1/posts: 마감·만료 공고가 섞여 나오고 순서가 정해져 있지 않아 Client(MatchingPage.tsx:65 find)가 옛 공고를 '내 공고'로 집는다. */
class PostListIT extends QaIntegrationSupport {
    @Autowired PostUseCase postUseCase;
    @Autowired GetPostUseCase getPostUseCase;

    @Test
    void recruiterSeesOnlyTheCurrentOpenPostAfterClosingAnEarlierOne() {
        var owner = member("모집자");
        var me = new MemberId(owner.getId());
        postUseCase.create(me, new CreatePostCommand(2, LocalDateTime.now().plusDays(1), "강남역", "첫 공고"));
        long first = posts.findAll().stream().filter(p -> p.getMember().getId().equals(owner.getId()))
                .mapToLong(p -> p.getId()).max().orElseThrow();
        postUseCase.close(me, new PostId(first));
        postUseCase.create(me, new CreatePostCommand(2, LocalDateTime.now().plusDays(1), "역삼역", "두 번째 공고"));
        // 만료 시각이 지났지만 아직 OPEN으로 남은 공고(지연 만료)
        var stale = savePost(owner, 2, Status.OPEN, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusDays(1));

        List<PostRes> mine = getPostUseCase.getPosts().stream()
                .filter(p -> p.author().authorId().equals(owner.getId())).toList();

        // Client와 같은 방식: 목록에서 작성자가 나인 첫 항목
        assertEquals("두 번째 공고", mine.get(0).message(), "Client가 고르는 '내 공고'");
        assertEquals(1, mine.size(), "마감(" + first + ")·만료(" + stale.getId() + ") 공고는 신청할 수 없으므로 목록에 없어야 한다");
    }
}
