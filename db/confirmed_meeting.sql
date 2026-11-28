-- Run once against the existing Spring database before starting the updated server.
CREATE TABLE confirmed_meeting (
    post_id BIGINT NOT NULL,
    restaurant VARCHAR(255) NOT NULL,
    PRIMARY KEY (post_id),
    CONSTRAINT fk_confirmed_meeting_post FOREIGN KEY (post_id) REFERENCES post (id)
);

-- 개별 초대 방의 확정 결과(초대당 1건). meeting_at은 Asia/Seoul 벽시계(post.meeting_at과 같은 규칙).
-- 계약: qa-cloud/contracts/invitation-result.md
CREATE TABLE confirmed_invitation_meeting (
    invitation_id BIGINT NOT NULL,
    meeting_at DATETIME(6) NOT NULL,
    location VARCHAR(255) NOT NULL,
    restaurant VARCHAR(255) NOT NULL,
    PRIMARY KEY (invitation_id),
    CONSTRAINT fk_confirmed_invitation_meeting_invitation FOREIGN KEY (invitation_id) REFERENCES invitation (id)
);
