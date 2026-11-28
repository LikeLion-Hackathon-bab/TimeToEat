-- F07: 온보딩·프로필 수정에서 직접 입력한 알레르기(customAllergyLabels). 서버를 새 버전으로 띄우기 전에 한 번 적용한다(ddl-auto=validate).
-- 형식은 Hibernate 6(MySQL 8)이 MemberCustomAllergy 엔티티에서 만드는 DDL과 같다.
CREATE TABLE IF NOT EXISTS member_custom_allergy (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    label VARCHAR(20) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_mca_member (member_id),
    CONSTRAINT fk_member_custom_allergy_member FOREIGN KEY (member_id) REFERENCES member (id)
);
