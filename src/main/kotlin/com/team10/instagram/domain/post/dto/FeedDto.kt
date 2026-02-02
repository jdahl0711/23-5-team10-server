package com.team10.instagram.domain.post.dto

import java.time.LocalDateTime

data class FeedResponse(
    val items: List<FeedPostDto>,
    val page: Int,
    val size: Int,
    val totalPages: Int,
    val totalElements: Long,
    val hasNext: Boolean,
    val hasPrev: Boolean,
)

data class FeedPostDto(
    val postId: Long,
    val author: FeedAuthorDto,
    val thumbnailImageUrl: String?,
    val likeCount: Long,
    val commentCount: Long,
    val createdAt: LocalDateTime,
    val isLiked: Boolean,
    val isBookmarked: Boolean,
)

data class FeedAuthorDto(
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
)
