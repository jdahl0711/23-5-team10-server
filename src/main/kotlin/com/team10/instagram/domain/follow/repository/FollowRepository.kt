package com.team10.instagram.domain.follow.repository

import com.team10.instagram.domain.follow.dto.FollowResponse
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Repository

@Repository
class FollowRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    // 1. 팔로우 여부 확인 : fromUserID가 toUserID를 팔로우 중이면 true, 아니면 false
    fun exists(
        fromUserId: Long,
        toUserId: Long,
    ): Boolean {
        val sql = "SELECT count(*) FROM follow WHERE from_user_id = ? AND to_user_id = ?"
        val count = jdbcTemplate.queryForObject(sql, Int::class.java, fromUserId, toUserId) ?: 0
        return count > 0
    }

    // 2. 팔로우 하기 (INSERT)
    fun save(
        fromUserId: Long,
        toUserId: Long,
    ) {
        val sql = "INSERT INTO follow (from_user_id, to_user_id) VALUES (?, ?)"
        jdbcTemplate.update(sql, fromUserId, toUserId)
    }

    // 3. 언팔로우 하기 (DELETE)
    fun delete(
        fromUserId: Long,
        toUserId: Long,
    ) {
        val sql = "DELETE FROM follow WHERE from_user_id = ? AND to_user_id = ?"
        jdbcTemplate.update(sql, fromUserId, toUserId)
    }

    // 4. 팔로워 목록 조회 (나를 팔로우 하는 사람들)
    fun findAllFollowers(
        targetUserId: Long,
        loginUserId: Long,
    ): List<FollowResponse> {
        val sql = """
            SELECT u.user_id, u.nickname, u.profile_image_url,
                   -- 내가 이 사람을 맞팔로우 하고 있는지 확인 (1이면 true, 0이면 false)
                   EXISTS(SELECT 1 FROM follow f2 WHERE f2.from_user_id = ? AND f2.to_user_id = u.user_id) as is_following
            FROM follow f
            JOIN users u ON f.from_user_id = u.user_id
            WHERE f.to_user_id = ?
        """

        return jdbcTemplate.query(sql, followRowMapper, loginUserId, targetUserId)
    }

    // 5. 팔로잉 목록 조회
    fun findAllFollowings(
        targetUserId: Long,
        loginUserId: Long,
    ): List<FollowResponse> {
        val sql = """
        SELECT u.user_id, u.nickname, u.profile_image_url,
               EXISTS(SELECT 1 FROM follow f2 WHERE f2.from_user_id = ? AND f2.to_user_id = u.user_id) as is_following
        FROM follow f
        JOIN users u ON f.to_user_id = u.user_id
        WHERE f.from_user_id = ?
    """
        return jdbcTemplate.query(sql, followRowMapper, loginUserId, targetUserId)
    }

    // 6. 팔로잉 목록 조회 (ID만)
    fun findAllFollowingIds(fromUserId: Long): List<Long> {
        val sql = "SELECT to_user_id FROM follow WHERE from_user_id = ?"

        return jdbcTemplate.queryForList(sql, Long::class.java, fromUserId)
    }

    // 7. 팔로잉 수 조회
    fun countFollowings(fromUserId: Long): Long {
        val sql = "SELECT count(*) FROM follow WHERE from_user_id = ?"
        return jdbcTemplate.queryForObject(sql, Long::class.java, fromUserId) ?: 0
    }

    // 8. 팔로워 수 조회
    fun countFollowers(toUserId: Long): Long {
        val sql = "SELECT count(*) FROM follow WHERE to_user_id = ?"
        return jdbcTemplate.queryForObject(sql, Long::class.java, toUserId) ?: 0
    }

    // DB 결과를 DTO로 변환
    private val followRowMapper =
        RowMapper { rs, _ ->
            FollowResponse(
                userId = rs.getLong("user_id"),
                nickname = rs.getString("nickname"),
                profileImageUrl = rs.getString("profile_image_url"),
                isFollowing = rs.getBoolean("is_following"),
            )
        }
}
