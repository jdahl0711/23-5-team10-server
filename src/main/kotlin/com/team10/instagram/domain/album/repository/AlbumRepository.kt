package com.team10.instagram.domain.album.repository

import com.team10.instagram.domain.album.dto.AlbumPostDto
import com.team10.instagram.domain.album.dto.AlbumResponse
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class AlbumRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    // 1. 앨범 생성
    fun save(
        userId: Long,
        title: String,
    ): Long {
        val sql = "INSERT INTO album (user_id, title) VALUES (?, ?)"
        jdbcTemplate.update(sql, userId, title)
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long::class.java)!!
    }

    // 2. 내 앨범 목록 조회 (썸네일 포함)
    fun findAllByUserId(userId: Long): List<AlbumResponse> {
        // 1) 실제 앨범 목록
        val albumSql = """
            SELECT a.album_id, a.title,
                   (SELECT COUNT(*) FROM post p WHERE p.album_id = a.album_id) as post_count,
                   (SELECT pi.image_url 
                    FROM post p 
                    JOIN post_image pi ON p.post_id = pi.post_id 
                    WHERE p.album_id = a.album_id 
                    -- 최신 글의 첫 번째 이미지를 가져옴
                    ORDER BY p.created_at DESC, pi.sort_order ASC 
                    LIMIT 1) as thumbnail_image_url
            FROM album a
            WHERE a.user_id = ?
            ORDER BY a.created_at DESC, a.album_id DESC
        """
        val realAlbums = jdbcTemplate.query(albumSql, albumResponseMapper, userId)

        // 2) '앨범 없음' 게시글 목록
        val unassignedSql = """
            SELECT 
                COUNT(*) as post_count,
                (SELECT pi.image_url 
                 FROM post p2 
                 JOIN post_image pi ON p2.post_id = pi.post_id 
                 WHERE p2.user_id = ? AND p2.album_id IS NULL 
                 ORDER BY p2.created_at DESC, pi.sort_order ASC 
                 LIMIT 1) as thumbnail_image_url
            FROM post p
            WHERE p.user_id = ? AND p.album_id IS NULL
        """

        val unassignedAlbum =
            jdbcTemplate.queryForObject(unassignedSql, { rs, _ ->
                val count = rs.getInt("post_count")

                AlbumResponse(
                    albumId = -1L,
                    title = "앨범 없음",
                    thumbnailImageUrl = rs.getString("thumbnail_image_url"),
                    postCount = count,
                )
            }, userId, userId)

        return listOf(unassignedAlbum!!) + realAlbums
    }

    // 3. 앨범 단건 조회 (제목 등 기본 정보) -> 검증용
    fun findById(albumId: Long): AlbumResponse? {
        val sql = "SELECT album_id, title, user_id FROM album WHERE album_id = ?"
        return try {
            jdbcTemplate.queryForObject(
                sql,
                { rs, _ ->
                    AlbumResponse(
                        albumId = rs.getLong("album_id"),
                        title = rs.getString("title"),
                        thumbnailImageUrl = null,
                        postCount = 0,
                    )
                },
                albumId,
            )
        } catch (e: Exception) {
            null
        }
    }

    // 3-1. 앨범 소유자 확인용 (User ID 조회)
    fun findOwnerId(albumId: Long): Long? {
        val sql = "SELECT user_id FROM album WHERE album_id = ?"
        return try {
            jdbcTemplate.queryForObject(sql, Long::class.java, albumId)
        } catch (e: Exception) {
            null
        }
    }

    // 4. 앨범 내부 게시글 목록 조회
    fun findPostsByAlbumId(albumId: Long): List<AlbumPostDto> {
        val sql = """
            SELECT p.post_id,
                   (SELECT pi.image_url 
                    FROM post_image pi 
                    WHERE pi.post_id = p.post_id 
                    ORDER BY pi.sort_order ASC
                    LIMIT 1) as image_url,

                    (SELECT COUNT(*) FROM post_like pl WHERE pl.post_id = p.post_id) as like_count,
                    (SELECT COUNT(*) FROM comment c WHERE c.post_id = p.post_id) as comment_count
                    
            FROM post p
            WHERE p.album_id = ?
            ORDER BY p.created_at DESC, p.post_id DESC
        """

        return jdbcTemplate.query(
            sql,
            { rs, _ ->
                AlbumPostDto(
                    postId = rs.getLong("post_id"),
                    imageUrl = rs.getString("image_url"),
                    likeCount = rs.getInt("like_count"),
                    commentCount = rs.getInt("comment_count"),
                )
            },
            albumId,
        )
    }

    // 5. 앨범 제목 수정
    fun updateTitle(
        albumId: Long,
        title: String,
    ) {
        val sql = "UPDATE album SET title = ? WHERE album_id = ?"
        jdbcTemplate.update(sql, title, albumId)
    }

    // 6. 게시글을 앨범에 추가/이동 (UPDATE)
    fun updatePostAlbum(
        postId: Long,
        albumId: Long?,
    ) {
        val sql = "UPDATE post SET album_id = ? WHERE post_id = ?"
        jdbcTemplate.update(sql, albumId, postId)
    }

    // 7. 앨범 삭제 전, 게시글 연결 끊기 (album_id = NULL)
    fun detachPostsFromAlbum(albumId: Long) {
        val sql = "UPDATE post SET album_id = NULL WHERE album_id = ?"
        jdbcTemplate.update(sql, albumId)
    }

    // 8. 앨범 삭제
    fun delete(albumId: Long) {
        val sql = "DELETE FROM album WHERE album_id = ?"
        jdbcTemplate.update(sql, albumId)
    }

    // 9.
    fun existsByUserIdAndTitle(
        userId: Long,
        title: String,
    ): Boolean {
        val sql = "SELECT COUNT(*) FROM album WHERE user_id = ? AND title = ?"
        val count = jdbcTemplate.queryForObject(sql, Int::class.java, userId, title) ?: 0
        return count > 0
    }

    // 10. 앨범 미지정 게시글 목록 조회
    fun findUnassignedPosts(userId: Long): List<AlbumPostDto> {
        val sql = """
            SELECT p.post_id,
                   -- 썸네일
                   (SELECT pi.image_url 
                    FROM post_image pi 
                    WHERE pi.post_id = p.post_id 
                    ORDER BY pi.sort_order ASC LIMIT 1) as image_url,
                    
                    (SELECT COUNT(*) FROM post_like pl WHERE pl.post_id = p.post_id) as like_count,
                    (SELECT COUNT(*) FROM comment c WHERE c.post_id = p.post_id) as comment_count
                   
            FROM post p
            WHERE p.user_id = ? 
              AND p.album_id IS NULL
            ORDER BY p.created_at DESC, p.post_id DESC
        """

        return jdbcTemplate.query(sql, { rs, _ ->
            AlbumPostDto(
                postId = rs.getLong("post_id"),
                imageUrl = rs.getString("image_url"),
                likeCount = rs.getInt("like_count"),
                commentCount = rs.getInt("comment_count"),
            )
        }, userId)
    }

    private val albumResponseMapper =
        RowMapper { rs: ResultSet, _: Int ->
            AlbumResponse(
                albumId = rs.getLong("album_id"),
                title = rs.getString("title"),
                postCount = rs.getInt("post_count"),
                thumbnailImageUrl = rs.getString("thumbnail_image_url"),
            )
        }
}
