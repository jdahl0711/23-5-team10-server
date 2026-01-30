package com.team10.instagram.domain.comment.repository

import com.team10.instagram.domain.comment.model.Comment
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface CommentRepository : CrudRepository<Comment, Long> {
    /* Apply pessimistic lock for transactional operations
     */
    @Query("SELECT * FROM comment WHERE comment_id = :id FOR UPDATE")
    fun findByIdWithLock(
        @Param("id") id: Long,
    ): Comment?

    /* Change created_at to comment_id because DATE_TIME precision issues on DB (seconds)
       Therefore, temporally use auto-increment property of Id
     */
    @Query("SELECT * FROM comment WHERE post_id = :postId ORDER BY comment_id DESC")
    fun findAllByPostIdOrderByCreatedAtDesc(
        @Param("postId") postId: Long,
    ): List<Comment>

    @Query("SELECT COUNT(*) FROM comment WHERE post_id = :postId")
    fun countByPostId(
        @Param("postId") postId: Long,
    ): Long
}
