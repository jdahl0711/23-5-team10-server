package com.team10.instagram.domain.user.controller

import com.team10.instagram.domain.auth.service.JwtTokenBlacklistService
import com.team10.instagram.domain.post.dto.PostResponse
import com.team10.instagram.domain.post.service.PostService
import com.team10.instagram.domain.user.LoggedInUser
import com.team10.instagram.domain.user.dto.UserDto
import com.team10.instagram.domain.user.model.User
import com.team10.instagram.domain.user.service.UserService
import com.team10.instagram.global.common.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User", description = "사용자 API")
class UserController(
    private val userService: UserService,
    private val postService: PostService,
    private val jwtTokenBlacklistService: JwtTokenBlacklistService,
) {
    @Operation(summary = "본인 정보 조회", description = "로그인한 사용자의 정보를 조회합니다")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "사용자 정보 조회 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패 (유효하지 않은 토큰)"),
        ],
    )
    @GetMapping("/me")
    fun me(
        @Parameter(hidden = true) @LoggedInUser user: User,
    ): ApiResponse<UserDto> = ApiResponse.onSuccess(UserDto(user))

    @GetMapping("/{userId}/posts")
    @Operation(summary = "유저 게시글 목록 조회", description = "특정 유저가 작성한 게시글 목록을 최신순으로 조회합니다.")
    fun getUserPosts(
        @LoggedInUser user: User,
        @PathVariable userId: Long,
    ): ApiResponse<Map<String, List<PostResponse>>> {
        val posts = postService.getPostsByUserId(user, userId)
        return ApiResponse.onSuccess(mapOf("posts" to posts))
    }

    @Operation(
        summary = "회원 탈퇴",
        description = "로그인한 사용자의 계정을 삭제하고, 현재 JWT Access Token을 무효화합니다",
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            SwaggerApiResponse(responseCode = "401", description = "인증 실패 (유효하지 않은 토큰)"),
        ],
    )
    @DeleteMapping("/me")
    fun deleteUser(
        @Parameter(hidden = true) @LoggedInUser user: User,
        @RequestHeader("Authorization") authorizationHeader: String,
    ): ApiResponse<String> {
        val token = authorizationHeader.replace("Bearer", "").trim()

        userService.deleteUser(user.userId!!)
        jwtTokenBlacklistService.add(token)

        return ApiResponse.onSuccess("Account deleted successfully")
    }
}
