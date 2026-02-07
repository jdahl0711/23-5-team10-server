package com.team10.instagram.domain.story.repository

import com.team10.instagram.domain.story.dto.StoryDetailResponse
import com.team10.instagram.domain.story.dto.StoryFeedResponse
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

@Repository
class StoryRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    // 1. 스토리 생성
    fun save(
        userId: Long,
        imageUrl: String,
    ) {
        val sql = "INSERT INTO story (user_id, image_url) VALUES (?, ?)"
        jdbcTemplate.update(sql, userId, imageUrl)
    }

    // 2. 스토리 피드 조회 (상단 바)
    // 조건: 내가 팔로우한 사람 + 24시간 이내 업로드 + 아직 안 본 게 있는지 체크
    fun findStoryFeed(loginUserId: Long): List<StoryFeedResponse> {
        val sql = """
            SELECT
                u.user_id,
                u.nickname,
                u.profile_image_url,
                -- 안 본 스토리가 하나라도 있으면 true, 다 봤으면 false
                -- 로직: (24시간 내 스토리 개수) > (내가 본 기록 개수)
                (
                    (SELECT COUNT(*) FROM story s
                     WHERE s.user_id = u.user_id AND s.created_at > NOW() - INTERVAL 1 DAY)
                    >
                    (SELECT COUNT(*) FROM story_view sv
                     JOIN story s ON sv.story_id = s.story_id
                     WHERE s.user_id = u.user_id AND sv.user_id = ? AND s.created_at > NOW() - INTERVAL 1 DAY)
                ) as has_unseen
            FROM users u
            JOIN follow f ON f.to_user_id = u.user_id
            WHERE f.from_user_id = ? -- 내가 팔로우한 사람
            -- 24시간 이내에 스토리를 올린 적이 있는 사람만 조회
            AND EXISTS (
                SELECT 1 FROM story s
                WHERE s.user_id = u.user_id AND s.created_at > NOW() - INTERVAL 1 DAY
            )
            ORDER BY has_unseen DESC, u.nickname ASC
        """

        return jdbcTemplate.query(sql, storyFeedMapper, loginUserId, loginUserId)
    }

    // 3. 특정 유저의 스토리 상세 목록 조회
    fun findAllByUserId(targetUserId: Long): List<StoryDetailResponse> {
        val sql = """
            SELECT s.story_id, s.user_id, s.image_url, s.created_at,
                   -- 조회수 계산
                   (SELECT COUNT(*) FROM story_view sv WHERE sv.story_id = s.story_id) as view_count
            FROM story s
            WHERE s.user_id = ?
              AND s.created_at > NOW() - INTERVAL 1 DAY -- 24시간 이내
            ORDER BY s.created_at ASC
        """
        return jdbcTemplate.query(sql, storyDetailMapper, targetUserId)
    }

    // 4. 스토리 읽음 처리
    fun saveView(
        userId: Long,
        storyId: Long,
    ) {
        // 이미 본 스토리는 중복 저장 안 함
        val sql = "INSERT IGNORE INTO story_view (user_id, story_id) VALUES (?, ?)"
        jdbcTemplate.update(sql, userId, storyId)
    }

    // 5. 스토리 존재 여부 및 주인 확인
    fun findOwnerId(storyId: Long): Long? {
        val sql = "SELECT user_id FROM story WHERE story_id = ?"
        return try {
            jdbcTemplate.queryForObject(sql, Long::class.java, storyId)
        } catch (e: Exception) {
            null
        }
    }

    // 6. 스토리 삭제
    fun delete(storyId: Long) {
        val sql = "DELETE FROM story WHERE story_id = ?"
        jdbcTemplate.update(sql, storyId)
    }

    // 내가 올린 스토리가 존재한다면, 내 정보를 StoryFeedResponse 형태로 반환
    fun findMyStoryFeedItem(userId: Long): StoryFeedResponse? {
        val sql = """
        SELECT
            u.user_id,
            u.nickname,
            u.profile_image_url,
            0 as has_unseen -- false
        FROM users u
        WHERE u.user_id = ?
        -- 내 스토리가 존재하는지 확인
        AND EXISTS (
            SELECT 1 FROM story s
            WHERE s.user_id = u.user_id AND s.created_at > NOW() - INTERVAL 1 DAY
        )
    """

        // 스토리가 없으면 null
        return try {
            jdbcTemplate.queryForObject(sql, storyFeedMapper, userId)
        } catch (e: Exception) {
            null
        }
    }

    // Mappers
    private val storyFeedMapper =
        RowMapper { rs, _ ->
            StoryFeedResponse(
                userId = rs.getLong("user_id"),
                nickname = rs.getString("nickname"),
                profileImageUrl = rs.getString("profile_image_url"),
                hasUnseenStory = rs.getInt("has_unseen") > 0, // 1이면 true
                stories = emptyList(),
            )
        }

    private val storyDetailMapper =
        RowMapper { rs, _ ->
            StoryDetailResponse(
                id = rs.getLong("story_id"),
                userId = rs.getLong("user_id"),
                imageUrl = rs.getString("image_url"),
                createdAt =
                    rs
                        .getTimestamp("created_at")
                        .toInstant()
                        .atZone(java.time.ZoneId.of("Asia/Seoul"))
                        .toLocalDateTime(),
                viewCount = rs.getInt("view_count"),
            )
        }
}
