--  Comment Like
CREATE TABLE IF NOT EXISTS comment_like (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (comment_id) REFERENCES comment (comment_id),
    FOREIGN KEY (user_id) REFERENCES users (user_id),
    UNIQUE (comment_id, user_id)
);