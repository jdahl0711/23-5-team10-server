package com.team10.instagram.domain.post.service

import com.team10.instagram.domain.comment.repository.CommentRepository
import com.team10.instagram.domain.follow.repository.FollowRepository
import com.team10.instagram.domain.post.dto.FeedAuthorDto
import com.team10.instagram.domain.post.dto.FeedPostDto
import com.team10.instagram.domain.post.dto.FeedResponse
import com.team10.instagram.domain.post.repository.BookmarkRepository
import com.team10.instagram.domain.post.repository.PostLikeRepository
import com.team10.instagram.domain.post.repository.PostRepository
import com.team10.instagram.domain.user.model.User
import com.team10.instagram.domain.user.repository.UserRepository
import com.team10.instagram.global.error.CustomException
import com.team10.instagram.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.ceil

@Service
class FeedService(
    private val postRepository: PostRepository,
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository,
    private val postLikeRepository: PostLikeRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val commentRepository: CommentRepository,
) {
    @Transactional(readOnly = true)
    fun getFeed(
        user: User,
        page: Int,
        size: Int,
    ): FeedResponse {
        val pageIndex = if (page < 1) 1 else page
        val offset = (pageIndex - 1) * size.toLong()

        // Retrieve all followers
        val followingIds = followRepository.findAllFollowingIds(user.userId!!)

        // Return empty result if followers is zero
        if (followingIds.isEmpty()) {
            return FeedResponse(
                items = emptyList(),
                page = pageIndex,
                size = size,
                totalPages = 0,
                totalElements = 0,
                hasNext = false,
                hasPrev = false,
            )
        }

        // Retrieve followers' posts
        val posts = postRepository.findAllByUserIdsIn(followingIds, size, offset)
        val totalElements = postRepository.countByUserIdsIn(followingIds)
        val totalPages = ceil(totalElements.toDouble() / size).toInt()

        // Transform to DTO
        val items =
            posts.map { post ->
                val author =
                    userRepository.findByUserId(post.userId)
                        ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

                val likeCount = postLikeRepository.countByPostId(post.id!!)
                val commentCount = commentRepository.countByPostId(post.id)
                val isLiked = postLikeRepository.existsByPostIdAndUserId(post.id, user.userId)
                val isBookmarked = bookmarkRepository.existsByPostIdAndUserId(post.id, user.userId)

                val thumbnail = post.images.firstOrNull()?.imageUrl

                FeedPostDto(
                    postId = post.id,
                    author =
                        FeedAuthorDto(
                            userId = author.userId!!,
                            nickname = author.nickname,
                            profileImageUrl = author.profileImageUrl,
                        ),
                    thumbnailImageUrl = thumbnail,
                    likeCount = likeCount,
                    commentCount = commentCount,
                    createdAt = post.createdAt!!,
                    isLiked = isLiked,
                    isBookmarked = isBookmarked,
                )
            }

        return FeedResponse(
            items = items,
            page = pageIndex,
            size = size,
            totalPages = totalPages,
            totalElements = totalElements,
            hasNext = pageIndex < totalPages,
            hasPrev = pageIndex > 1,
        )
    }
}
