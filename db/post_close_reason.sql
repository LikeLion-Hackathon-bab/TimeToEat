-- 공고 마감 사유(검증 14 O1): post.close_reason. Client 가 '마감'(작성자 마감)과 '기간 만료'를 구분하게 한다.
-- 새 DB를 Hibernate가 만들면 이미 들어 있으므로 필요 없다. 기존 DB에만 서버 배포 전에 한 번 적용한다(ddl-auto=validate).
-- 값: MANUAL(작성자 마감) | FULL(인원 충족) | EXPIRED(기간 만료). OPEN 공고는 NULL.
ALTER TABLE post ADD COLUMN close_reason ENUM('EXPIRED','FULL','MANUAL') NULL; -- Hibernate 6가 만드는 순서와 같다

-- 이미 마감된 옛 공고를 추정해 채운다. close()는 expired_at 을 마감 시각으로 덮어쓰고 만료는 그대로 두므로,
-- expired_at 이 생성+2시간 이상이면 만료로 본다(다시 모집 뒤 작성자가 마감한 드문 경우는 만료로 잘못 볼 수 있다).
UPDATE post SET close_reason = 'EXPIRED'
 WHERE status <> 'OPEN' AND close_reason IS NULL AND expired_at >= created_at + INTERVAL 2 HOUR;
UPDATE post p SET close_reason = 'FULL'
 WHERE p.status <> 'OPEN' AND p.close_reason IS NULL
   AND (SELECT COUNT(*) FROM participation a WHERE a.post_id = p.id) >= p.target_count;
UPDATE post SET close_reason = 'MANUAL'
 WHERE status <> 'OPEN' AND close_reason IS NULL;
