-- F25 알림 종류 추가(기획 문서 §14.2 일반 알림 14행). db/notification.sql 을 이미 적용한 DB에 한 번 적용한다.
-- 값을 더하는 변경이다. 기존 행의 문자열 값은 그대로 남고, 다시 실행해도 같은 결과(멱등)다(값 순서가 바뀌어 MySQL이 테이블을 다시 쓰지만 알림 테이블은 작다).
-- 새 DB는 db/notification.sql 에 같은 목록이 이미 있으므로 이 파일이 필요 없다(적용해도 무해).
ALTER TABLE notification MODIFY COLUMN type ENUM('ANNOUNCEMENT_JOINED','AREA_NEWS','CHALLENGE_ACHIEVED','COMMENT_CREATED','COMMERCIAL_NEWS',
              'COUPON_ISSUED','DISCOUNT_NEWS','INVITATION_ACCEPTED','INVITATION_REJECTED','INVITATION_SENT',
              'MATCHING_COMPLETED','MEAL_NEWS','MEETING_CONFIRMED','MEETING_COORDINATING','MEETING_REMINDER',
              'POST_LIKED','POST_TAGGED','REFERRAL_ACTIVITY','REPLY_CREATED','SCHOOL_NEWS') NOT NULL;
