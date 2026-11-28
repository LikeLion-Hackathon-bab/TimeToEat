-- 쿠폰함(F75·F76)을 Spring 메모리에서 DB로 옮김. 서버를 새 버전으로 띄우기 전에 한 번 적용한다(ddl-auto=validate).
-- 형식은 Hibernate 6(MySQL 8)이 MemberCoupon 엔티티에서 만드는 DDL과 같다.
-- 이전 버전의 메모리 쿠폰(받은 보상 쿠폰·사용 처리)은 옮길 데이터가 없다(재시작 때마다 사라졌음).
-- 시연 쿠폰 3장은 회원이 쿠폰함을 처음 열 때 회원별로 한 번 만들어진다(seed_key DEMO_1..3).
CREATE TABLE IF NOT EXISTS member_coupon (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    seed_key VARCHAR(32) NULL,                     -- 시연 쿠폰 키, 보상 쿠폰은 NULL(유니크에서 NULL은 겹쳐도 됨)
    title VARCHAR(100) NOT NULL,
    shop_name VARCHAR(100) NOT NULL,
    type ENUM('DISCOUNT','SERVICE') NOT NULL,
    condition_text VARCHAR(255) NOT NULL,
    expires_at DATE NOT NULL,                      -- 이 날짜까지 사용 가능(KST, 당일 포함)
    used_at DATETIME(6) NULL,                      -- NULL = 사용 전
    thumbnail_url VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_coupon_seed (member_id, seed_key),
    KEY idx_member_coupon_member (member_id),
    CONSTRAINT fk_member_coupon_member FOREIGN KEY (member_id) REFERENCES member (id)
);
