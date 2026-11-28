-- 쿠폰함(F75·F76)을 Spring 메모리에서 DB로 옮김. 서버를 새 버전으로 띄우기 전에 한 번 적용한다(ddl-auto=validate).
-- 형식은 Hibernate 6(MySQL 8)이 MemberCoupon 엔티티에서 만드는 DDL과 같다.
-- 이전 버전의 메모리 쿠폰(받은 보상 쿠폰·사용 처리)은 옮길 데이터가 없다(재시작 때마다 사라졌음).
-- 실제로 발급된 쿠폰(챌린지 보상·추천 INVITE)만 들어간다. 시연용 가짜 쿠폰은 없다.
CREATE TABLE IF NOT EXISTS member_coupon (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    shop_name VARCHAR(100) NOT NULL,
    type ENUM('DISCOUNT','SERVICE') NOT NULL,
    condition_text VARCHAR(255) NOT NULL,
    expires_at DATE NOT NULL,                      -- 이 날짜까지 사용 가능(KST, 당일 포함)
    used_at DATETIME(6) NULL,                      -- NULL = 사용 전
    thumbnail_url VARCHAR(500) NULL,               -- 보상 쿠폰은 NULL
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_member_coupon_member (member_id),
    CONSTRAINT fk_member_coupon_member FOREIGN KEY (member_id) REFERENCES member (id)
);
