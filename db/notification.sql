-- 알림함(SCR-25·26)과 웹 푸시 토큰. 서버를 새 버전으로 띄우기 전에 한 번 적용한다(ddl-auto=validate).
-- 출처: ai-notify 작업자 패치. 형식은 Hibernate 6(MySQL 8)이 엔티티에서 만드는 DDL과 같다.
CREATE TABLE notification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    receiver_id BIGINT NULL,                       -- LOCAL_NEWS 전체 공개 소식은 NULL
    category ENUM('ALARM','LOCAL_NEWS') NOT NULL,
    type ENUM('AREA_NEWS','COMMERCIAL_NEWS','DISCOUNT_NEWS','INVITATION_ACCEPTED','INVITATION_REJECTED',
              'INVITATION_SENT','MATCHING_COMPLETED','MEAL_NEWS','SCHOOL_NEWS') NOT NULL,
    title VARCHAR(100) NOT NULL,
    body VARCHAR(500) NOT NULL,
    url VARCHAR(255) NULL,
    image_url VARCHAR(500) NULL,
    period VARCHAR(100) NULL,
    is_read BIT(1) NOT NULL,
    read_at DATETIME(6) NULL,
    dedup_key VARCHAR(120) NULL,                   -- RabbitMQ 재전달 시 같은 알림 중복 방지
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_dedup_key (dedup_key),
    KEY idx_notification_receiver_category (receiver_id, category, id)
);

CREATE TABLE push_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL,
    platform VARCHAR(32) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_push_token_token (token),
    KEY idx_push_token_member (member_id)
);
