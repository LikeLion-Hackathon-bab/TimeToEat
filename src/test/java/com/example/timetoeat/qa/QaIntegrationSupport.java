package com.example.timetoeat.qa;

import com.example.timetoeat.domain.member.entity.MemberEntity;
import com.example.timetoeat.domain.member.repository.MemberJpaRepository;
import com.example.timetoeat.domain.post.adap.out.persistence.PostEntity;
import com.example.timetoeat.domain.post.adap.out.persistence.PostRepository;
import com.example.timetoeat.domain.post.application.service.MatchingPostService;
import com.example.timetoeat.domain.post.domain.Status;
import com.example.timetoeat.global.auth.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 실제 MySQL·Redis·RabbitMQ 컨테이너에 붙는 통합 테스트 공통 설정.
 * QA_IT=1 과 DB_URL 등 환경변수가 있을 때만 실행된다(CI의 `build -x test`와 무관).
 * Socket 서버 호출(MatchingPostService)은 시험 대역으로 바꾼다.
 */
@SpringBootTest
@QaIT // @Inherited: 하위 *IT 에도 적용(직접 쓴 @EnabledIfEnvironmentVariable 은 상속되지 않았다)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.show-sql=false",
        // 컨텍스트 캐시 때문에 여러 컨텍스트가 같은 큐를 나눠 소비하지 않도록 기본은 리스너를 끈다
        "spring.rabbitmq.listener.simple.auto-startup=false",
        // 만료 스케줄러가 다른 시험의 공고를 몰래 마감하지 않게 끈다. 시험은 PostExpirySweeper.sweep()을 직접 부른다
        "post.expiry.scheduler.enabled=false",
        "logging.level.org.hibernate.SQL=warn"
})
public abstract class QaIntegrationSupport {
    @Autowired protected MemberJpaRepository members;
    @Autowired protected PostRepository posts;
    @Autowired protected JdbcTemplate jdbc;
    @MockitoBean protected MatchingPostService socket;

    protected MemberEntity member(String name) {
        return members.save(MemberEntity.builder()
                .username(name)
                .email(name + "-" + UUID.randomUUID() + "@qa.test")
                .role(Role.ROLE_MEMBER)
                .build());
    }

    protected PostEntity savePost(MemberEntity owner, int target, Status status, LocalDateTime expiredAt, LocalDateTime meetingAt) {
        return posts.save(new PostEntity(null, target, status, LocalDateTime.now(), meetingAt, expiredAt,
                "강남역", "점심 같이", null, owner));
    }

    protected PostEntity openPost(MemberEntity owner, int target) {
        return savePost(owner, target, Status.OPEN, LocalDateTime.now().plusHours(2), LocalDateTime.now().plusDays(1));
    }

    protected int participationRows(long postId) {
        return jdbc.queryForObject("select count(*) from participation where post_id=?", Integer.class, postId);
    }
}
