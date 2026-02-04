-- 1. 성능 향상을 위한 인덱스 추가 (to_user_id 조회용)
CREATE INDEX idx_follow_to_user ON follow (to_user_id);

-- 2. 기존 외래키 삭제
ALTER TABLE follow DROP FOREIGN KEY follow_ibfk_1; -- from_user_id 제약조건
ALTER TABLE follow DROP FOREIGN KEY follow_ibfk_2; -- to_user_id 제약조건

-- 3. ON DELETE CASCADE 적용
ALTER TABLE follow
    ADD CONSTRAINT fk_follow_from_user
        FOREIGN KEY (from_user_id) REFERENCES users (user_id) ON DELETE CASCADE;

ALTER TABLE follow
    ADD CONSTRAINT fk_follow_to_user
        FOREIGN KEY (to_user_id) REFERENCES users (user_id) ON DELETE CASCADE;
