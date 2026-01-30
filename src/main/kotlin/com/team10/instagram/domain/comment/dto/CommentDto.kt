package com.team10.instagram.domain.comment.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class CommentCreateRequest(
    @Schema(description = "댓글 내용", example = "좋은 사진이네요!")
    @field:Size(max = 1000, message = "댓글은 1000자 이하여야 합니다.")
    val content: String,
)

data class CommentUpdateRequest(
    @Schema(description = "수정할 댓글 내용", example = "수정된 댓글입니다.")
    @field:Size(max = 1000, message = "댓글은 1000자 이하여야 합니다.")
    val content: String,
)

data class CommentResponse(
    val id: Long,
    val postId: Long,
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val likeCount: Long,
    val isLiked: Boolean,
    val likedUserIds: List<Long>,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
