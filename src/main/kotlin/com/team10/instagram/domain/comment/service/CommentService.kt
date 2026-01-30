package com.team10.instagram.domain.comment.service

import com.team10.instagram.domain.comment.dto.CommentCreateRequest
import com.team10.instagram.domain.comment.dto.CommentResponse
import com.team10.instagram.domain.comment.dto.CommentUpdateRequest
import com.team10.instagram.domain.comment.model.Comment
import com.team10.instagram.domain.comment.model.CommentLike
import com.team10.instagram.domain.comment.repository.CommentLikeRepository
import com.team10.instagram.domain.comment.repository.CommentRepository
import com.team10.instagram.domain.post.repository.PostRepository
import com.team10.instagram.domain.user.model.User
import com.team10.instagram.domain.user.repository.UserRepository
import com.team10.instagram.global.error.CustomException
import com.team10.instagram.global.error.ErrorCode
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val commentLikeRepository: CommentLikeRepository,
) {
    @Transactional
    fun create(
        user: User,
        postId: Long,
        request: CommentCreateRequest,
    ): CommentResponse {
        if (!postRepository.existsById(postId)) {
            throw CustomException(ErrorCode.POST_NOT_FOUND)
        }

        if (request.content.isBlank()) {
            throw CustomException(ErrorCode.EMPTY_CONTENT)
        }

        val comment =
            Comment(
                postId = postId,
                userId = user.userId!!,
                content = request.content,
            )

        val savedComment = commentRepository.save(comment)
        return convertToDto(savedComment, user, user)
    }

    @Transactional(readOnly = true)
    fun getCommentsByPostId(
        currentUser: User,
        postId: Long,
    ): List<CommentResponse> {
        if (!postRepository.existsById(postId)) {
            throw CustomException(ErrorCode.POST_NOT_FOUND)
        }

        val comments = commentRepository.findAllByPostIdOrderByCreatedAtDesc(postId)
        return comments.map { comment ->
            val writer =
                userRepository.findByUserId(comment.userId)
                    ?: throw CustomException(ErrorCode.USER_NOT_FOUND)
            convertToDto(comment, writer, currentUser)
        }
    }

    @Transactional
    fun update(
        user: User,
        commentId: Long,
        request: CommentUpdateRequest,
    ): CommentResponse {
        val comment =
            commentRepository.findByIdOrNull(commentId)
                ?: throw CustomException(ErrorCode.COMMENT_NOT_FOUND)

        if (comment.userId != user.userId) throw CustomException(ErrorCode.ACCESS_DENIED)

        if (request.content.isBlank()) {
            throw CustomException(ErrorCode.EMPTY_CONTENT)
        }

        val updatedComment = comment.copy(content = request.content)
        val saved = commentRepository.save(updatedComment)

        return convertToDto(saved, user, user)
    }

    @Transactional
    fun delete(
        user: User,
        commentId: Long,
    ) {
        val comment =
            commentRepository.findByIdOrNull(commentId)
                ?: throw CustomException(ErrorCode.COMMENT_NOT_FOUND)

        if (comment.userId != user.userId) throw CustomException(ErrorCode.ACCESS_DENIED)

        commentRepository.delete(comment)
    }

    @Transactional
    fun likeComment(
        user: User,
        commentId: Long,
    ) {
        // Apply pessimistic lock: Queueing duplicate requests
        val comment =
            commentRepository.findByIdWithLock(commentId)
                ?: throw CustomException(ErrorCode.COMMENT_NOT_FOUND)

        // return 200 OK for duplicate requests
        if (commentLikeRepository.existsByCommentIdAndUserId(commentId, user.userId!!)) {
            return
        }

        commentLikeRepository.save(CommentLike(commentId = commentId, userId = user.userId))
    }

    @Transactional
    fun unlikeComment(
        user: User,
        commentId: Long,
    ) {
        if (!commentRepository.existsById(commentId)) throw CustomException(ErrorCode.COMMENT_NOT_FOUND)

        val like = commentLikeRepository.findByCommentIdAndUserId(commentId, user.userId!!)
        if (like != null) commentLikeRepository.delete(like)
    }

    private fun convertToDto(
        comment: Comment,
        writer: User,
        currentUser: User?,
    ): CommentResponse {
        val likeCount = commentLikeRepository.countByCommentId(comment.id!!)

        val isLiked =
            currentUser?.let {
                commentLikeRepository.existsByCommentIdAndUserId(comment.id, it.userId!!)
            } ?: false

        val likedUserIds = commentLikeRepository.findUserIdsByCommentId(comment.id)

        return CommentResponse(
            id = comment.id,
            postId = comment.postId,
            userId = writer.userId!!,
            nickname = writer.nickname,
            content = comment.content,
            profileImageUrl = writer.profileImageUrl,
            likeCount = likeCount,
            isLiked = isLiked,
            likedUserIds = likedUserIds,
            createdAt = comment.createdAt ?: LocalDateTime.now(),
            updatedAt = comment.updatedAt ?: LocalDateTime.now(),
        )
    }
}
