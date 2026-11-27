-- Run once against the existing Spring database before starting the updated server.
CREATE TABLE confirmed_meeting (
    post_id BIGINT NOT NULL,
    restaurant VARCHAR(255) NOT NULL,
    PRIMARY KEY (post_id),
    CONSTRAINT fk_confirmed_meeting_post FOREIGN KEY (post_id) REFERENCES post (id)
);
