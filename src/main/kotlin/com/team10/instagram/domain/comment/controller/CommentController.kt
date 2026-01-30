package com.team10.instagram.domain.comment.controller

import com.team10.instagram.domain.comment.dto.CommentCreateRequest
import com.team10.instagram.domain.comment.dto.CommentResponse
import com.team10.instagram.domain.comment.dto.CommentUpdateRequest
import com.team10.instagram.domain.comment.service.CommentService
import com.team10.instagram.domain.user.LoggedInUser
import com.team10.instagram.domain.user.model.User
import com.team10.instagram.global.common.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/posts")
@Tag(name = "Comment API", description = "댓글 관련 API")
class CommentController(
    private val commentService: CommentService,
) {
    @PostMapping("/{postId}/comments")
    @Operation(summary = "댓글 생성", description = "특정 게시글에 댓글을 작성합니다.")
    fun createComment(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @PathVariable postId: Long,
        @Valid @RequestBody request: CommentCreateRequest,
    ): ApiResponse<CommentResponse> = ApiResponse.onSuccess(commentService.create(user, postId, request))

    @GetMapping("/{postId}/comments")
    @Operation(summary = "댓글 목록 조회", description = "게시글의 댓글 목록을 조회합니다.")
    fun getComments(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @PathVariable postId: Long,
    ): ApiResponse<List<CommentResponse>> = ApiResponse.onSuccess(commentService.getCommentsByPostId(user, postId))

    @PutMapping("/{postId}/comments/{commentId}")
    @Operation(summary = "댓글 수정", description = "댓글 내용을 수정합니다.")
    fun updateComment(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @PathVariable postId: Long, // URL 경로 통일성을 위해 존재하지만 Service에는 ID만 전달
        @PathVariable commentId: Long,
        @Valid @RequestBody request: CommentUpdateRequest,
    ): ApiResponse<CommentResponse> = ApiResponse.onSuccess(commentService.update(user, commentId, request))

    @DeleteMapping("/{postId}/comments/{commentId}")
    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다.")
    fun deleteComment(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
    ): ApiResponse<Unit> {
        commentService.delete(user, commentId)
        return ApiResponse.onSuccess(Unit)
    }

    @PostMapping("/{postId}/comments/{commentId}/like")
    @Operation(summary = "댓글 좋아요", description = "댓글에 좋아요를 누릅니다.")
    fun likeComment(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
    ): ApiResponse<Unit> {
        commentService.likeComment(user, commentId)
        return ApiResponse.onSuccess(Unit)
    }

    @DeleteMapping("/{postId}/comments/{commentId}/like")
    @Operation(summary = "댓글 좋아요 취소", description = "댓글 좋아요를 취소합니다.")
    fun unlikeComment(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
    ): ApiResponse<Unit> {
        commentService.unlikeComment(user, commentId)
        return ApiResponse.onSuccess(Unit)
    }
}
