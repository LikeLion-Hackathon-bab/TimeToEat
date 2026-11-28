-- 초대 대기실 나가기(F46): invitation.status 에 CANCELLED 추가.
-- 새 DB를 Hibernate가 만들면 이미 들어 있으므로 필요 없다. 기존 DB(Hibernate가 enum 컬럼을 만든 DB)에만 한 번 적용한다.
-- Hibernate 6은 @Enumerated(STRING)을 MySQL ENUM 컬럼으로 만들고, ddl-auto=update/validate는 값 목록을 고치지도 검사하지도 않는다.
-- 이 ALTER 없이 새 서버를 띄우면 취소 때 "Data truncated for column 'status'" 오류가 난다.
ALTER TABLE invitation MODIFY status ENUM('APPROVED','CANCELLED','PENDING','REJECTED') NULL;
