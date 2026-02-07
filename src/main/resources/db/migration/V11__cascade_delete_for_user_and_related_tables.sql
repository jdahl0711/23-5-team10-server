-- 2. album.user_id -> users.user_id
ALTER TABLE album DROP FOREIGN KEY album_ibfk_1;
ALTER TABLE album
    ADD CONSTRAINT album_ibfk_1
        FOREIGN KEY (user_id) REFERENCES users(user_id)
            ON DELETE CASCADE;

-- 3. post.user_id -> users.user_id
ALTER TABLE post DROP FOREIGN KEY post_ibfk_1;
ALTER TABLE post
    ADD CONSTRAINT post_ibfk_1
        FOREIGN KEY (user_id) REFERENCES users(user_id)
            ON DELETE CASCADE;

-- 4. post.album_id -> album.album_id
ALTER TABLE post DROP FOREIGN KEY post_ibfk_2;
ALTER TABLE post
    ADD CONSTRAINT post_ibfk_2
        FOREIGN KEY (album_id) REFERENCES album(album_id)
            ON DELETE CASCADE;

-- 5. post_image.post_id -> post.post_id
ALTER TABLE post_image DROP FOREIGN KEY post_image_ibfk_1;
ALTER TABLE post_image
    ADD CONSTRAINT post_image_ibfk_1
        FOREIGN KEY (post_id) REFERENCES post(post_id)
            ON DELETE CASCADE;

-- 6. comment.user_id -> users.user_id
ALTER TABLE comment DROP FOREIGN KEY comment_ibfk_2;
ALTER TABLE comment
    ADD CONSTRAINT comment_ibfk_2
        FOREIGN KEY (user_id) REFERENCES users(user_id)
            ON DELETE CASCADE;

-- 7. comment.post_id -> post.post_id
ALTER TABLE comment DROP FOREIGN KEY comment_ibfk_1;
ALTER TABLE comment
    ADD CONSTRAINT comment_ibfk_1
        FOREIGN KEY (post_id) REFERENCES post(post_id)
            ON DELETE CASCADE;

-- 8. post_like.user_id -> users.user_id
ALTER TABLE post_like DROP FOREIGN KEY post_like_ibfk_2;
ALTER TABLE post_like
    ADD CONSTRAINT post_like_ibfk_2
        FOREIGN KEY (user_id) REFERENCES users(user_id)
            ON DELETE CASCADE;

-- 9. post_like.post_id -> post.post_id
ALTER TABLE post_like DROP FOREIGN KEY post_like_ibfk_1;
ALTER TABLE post_like
    ADD CONSTRAINT post_like_ibfk_1
        FOREIGN KEY (post_id) REFERENCES post(post_id)
            ON DELETE CASCADE;

-- 10. story.user_id -> users.user_id
ALTER TABLE story DROP FOREIGN KEY story_ibfk_1;
ALTER TABLE story
    ADD CONSTRAINT story_ibfk_1
        FOREIGN KEY (user_id) REFERENCES users(user_id)
            ON DELETE CASCADE;

-- 11. bookmark.user_id -> users.user_id
ALTER TABLE bookmark DROP FOREIGN KEY bookmark_ibfk_2;
ALTER TABLE bookmark
    ADD CONSTRAINT bookmark_ibfk_2
        FOREIGN KEY (user_id) REFERENCES users(user_id)
            ON DELETE CASCADE;

-- 12. bookmark.post_id -> post.post_id
ALTER TABLE bookmark DROP FOREIGN KEY bookmark_ibfk_1;
ALTER TABLE bookmark
    ADD CONSTRAINT bookmark_ibfk_1
        FOREIGN KEY (post_id) REFERENCES post(post_id)
            ON DELETE CASCADE;

-- 13. story_view.user_id -> users.user_id
ALTER TABLE story_view DROP FOREIGN KEY story_view_ibfk_2;
ALTER TABLE story_view
    ADD CONSTRAINT story_view_ibfk_2
        FOREIGN KEY (user_id) REFERENCES users(user_id)
            ON DELETE CASCADE;

-- 14. story_view.story_id -> story.story_id
ALTER TABLE story_view DROP FOREIGN KEY story_view_ibfk_1;
ALTER TABLE story_view
    ADD CONSTRAINT story_view_ibfk_1
        FOREIGN KEY (story_id) REFERENCES story(story_id)
            ON DELETE CASCADE;

-- 15. search_history.from_user_id -> users.user_id
ALTER TABLE search_history DROP FOREIGN KEY search_history_ibfk_1;
ALTER TABLE search_history
    ADD CONSTRAINT search_history_ibfk_1
        FOREIGN KEY (from_user_id) REFERENCES users(user_id)
            ON DELETE CASCADE;

-- 16. search_history.to_user_id -> users.user_id
ALTER TABLE search_history DROP FOREIGN KEY search_history_ibfk_2;
ALTER TABLE search_history
    ADD CONSTRAINT search_history_ibfk_2
        FOREIGN KEY (to_user_id) REFERENCES users(user_id)
            ON DELETE CASCADE;

-- 17. comment_like.user_id -> users.user_id
ALTER TABLE comment_like DROP FOREIGN KEY comment_like_ibfk_2;
ALTER TABLE comment_like
    ADD CONSTRAINT comment_like_ibfk_2
        FOREIGN KEY (user_id) REFERENCES users(user_id)
            ON DELETE CASCADE;

-- 18. comment_like.comment_id -> comment.comment_id
ALTER TABLE comment_like DROP FOREIGN KEY comment_like_ibfk_1;
ALTER TABLE comment_like
    ADD CONSTRAINT comment_like_ibfk_1
        FOREIGN KEY (comment_id) REFERENCES comment(comment_id)
            ON DELETE CASCADE;
