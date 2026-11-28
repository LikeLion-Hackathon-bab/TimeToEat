package com.example.timetoeat;

import com.example.timetoeat.qa.QaIT;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 리스너는 띄우지 않는다: 캐시된 이 컨텍스트가 통합 테스트의 큐 메시지를 가로채지 않도록
// 전체 컨텍스트는 MySQL·Redis·RabbitMQ가 있어야 뜬다 → 통합시험과 같은 조건(QA_IT=1)
@QaIT
// 스키마는 통합시험과 같이 시험 DB에 새로 만든다(운영 프로필의 validate 는 빈 DB에서 실패)
@SpringBootTest(properties = {"spring.rabbitmq.listener.simple.auto-startup=false", "spring.jpa.hibernate.ddl-auto=create"})
class TimeToEatApplicationTests {

    @Test
    void contextLoads() {
    }

}
