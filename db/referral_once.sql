-- 추천 코드 등록을 회원당 1회로(T2). 서버를 새 버전으로 띄우기 전에 한 번 적용한다(ddl-auto=validate 는 유니크를 검사하지 않지만
-- 동시 요청 방어는 이 제약이 한다). NULL(아직 안 쓴 코드)은 여러 행이어도 된다.
-- 적용 전 확인: 이미 두 번 이상 등록한 회원이 있으면 아래 ALTER 가 실패한다.
--   SELECT used_by_id, COUNT(*) FROM referral_code WHERE used_by_id IS NOT NULL GROUP BY used_by_id HAVING COUNT(*) > 1;
-- 있으면 회원별로 가장 이른 등록(used_at 최소)만 남기고 나머지 행의 used_by_id·used_at 을 NULL 로 되돌린 뒤 적용한다
-- (이미 지급된 INVITE 쿠폰은 그대로 둔다).
ALTER TABLE referral_code ADD CONSTRAINT uk_referral_code_used_by UNIQUE (used_by_id);
