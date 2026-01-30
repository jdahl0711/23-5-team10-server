package com.team10.instagram.domain.comment.repository

import com.team10.instagram.domain.comment.model.CommentLike
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface CommentLikeRepository : CrudRepository<CommentLike, Long> {
    fun countByCommentId(commentId: Long): Long

    fun existsByCommentIdAndUserId(
        commentId: Long,
        userId: Long,
    ): Boolean

    fun findByCommentIdAndUserId(
        commentId: Long,
        userId: Long,
    ): CommentLike?

    @Query("SELECT user_id FROM comment_like WHERE comment_id = :commentId")
    fun findUserIdsByCommentId(
        @Param("commentId") commentId: Long,
    ): List<Long>

    @Query("DELETE FROM comment_like WHERE comment_id = :commentId")
    fun deleteAllByCommentId(
        @Param("commentId") commentId: Long,
    )
}
