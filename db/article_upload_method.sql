-- 게시물 사진을 올린 방법(CAMERA/ALBUM). 서버를 새 버전으로 띄우기 전에 한 번 적용한다(ddl-auto=validate).
-- 기존 글은 NULL. ENUM이 아니라 VARCHAR라 값이 늘어도 ALTER가 필요 없다. 형식은 Hibernate 6(MySQL 8)이 엔티티에서 만드는 DDL과 같다.
-- 여러 번 실행해도 된다(열이 있으면 건너뜀).
SET @has_col := (SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'article' AND column_name = 'upload_method');
SET @ddl := IF(@has_col = 0, 'ALTER TABLE article ADD COLUMN upload_method VARCHAR(16) NULL', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
