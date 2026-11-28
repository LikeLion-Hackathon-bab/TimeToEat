package com.example.timetoeat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// 리스너는 띄우지 않는다: 캐시된 이 컨텍스트가 통합 테스트의 큐 메시지를 가로채지 않도록
@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
class TimeToEatApplicationTests {

    @Test
    void contextLoads() {
    }

}
