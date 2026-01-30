package com.team10.instagram.domain.comment.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("comment_like")
data class CommentLike(
    @Id
    val id: Long? = null,
    @Column("comment_id")
    val commentId: Long,
    @Column("user_id")
    val userId: Long,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
