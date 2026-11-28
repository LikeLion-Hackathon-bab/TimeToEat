package com.example.timetoeat.qa;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.lang.annotation.*;

/**
 * 실제 MySQL·Redis·RabbitMQ가 필요한 시험 표시. QA_IT=1 일 때만 실행되고, 아니면 건너뛴다.
 * JUnit 조건 애너테이션(@EnabledIfEnvironmentVariable)은 상위 클래스에서 상속되지 않으므로,
 * {@link Inherited} 메타 애너테이션으로 감싸 QaIntegrationSupport 하위 클래스 전부에 적용되게 한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@EnabledIfEnvironmentVariable(named = "QA_IT", matches = "1")
public @interface QaIT {
}
