package com.team10.instagram.domain.user.dto

import com.team10.instagram.domain.user.model.User
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class UserDto(
    @Schema(description = "사용자 식별을 위한 아이디", example = "1")
    val userId: Long,
    @Schema(description = "사용자 이메일", example = "test@gmail.com")
    val email: String,
    @Schema(description = "사용자 닉네임", example = "tester")
    val nickname: String,
    @Schema(description = "사용자 이름", example = "홍길동")
    val name: String?,
    @Schema(description = "사용자 프로필 이미지 URL", example = "https://example.com/profile.png", nullable = true)
    val profileImageUrl: String?,
    @Schema(description = "사용자 소개", example = "안녕하세요, 저는 테스터입니다.", nullable = true)
    val bio: String?,
    @Schema(description = "사용자 역할", example = "USER")
    val role: String,
) {
    constructor(user: User) : this(
        userId = user.userId!!,
        email = user.email,
        nickname = user.nickname,
        name = user.name,
        profileImageUrl = user.profileImageUrl,
        bio = user.bio,
        role = user.role.name,
    )
}

data class UserSearchRequest(
    @field:NotBlank
    @Schema(description = "검색어", example = "tester")
    val q: String,
)

data class UserSearchResponseDtoUnit(
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    val name: String?,
    val isFollowed: Boolean,
)

data class UserSearchResponse(
    val users: List<UserSearchResponseDtoUnit>,
)

data class ProfileResponse(
    @Schema(description = "사용자 식별을 위한 아이디", example = "1")
    val userId: Long,
    @Schema(description = "사용자 닉네임", example = "tester")
    val nickname: String,
    @Schema(description = "사용자 이름", example = "홍길동")
    val name: String?,
    @Schema(description = "사용자 소개", example = "안녕하세요, 저는 테스터입니다.", nullable = true)
    val bio: String?,
    @Schema(description = "사용자 프로필 이미지 URL", example = "https://example.com/profile.png", nullable = true)
    val profileImageUrl: String?,
    val postCount: Long,
    val followerCount: Long,
    val followingCount: Long,
    val isMe: Boolean,
    val isFollowed: Boolean,
)

data class ProfilePatchRequest(
    @Schema(description = "사용자 닉네임", example = "tester")
    val nickname: String? = null,
    @Schema(description = "사용자 이름", example = "홍길동")
    val name: String? = null,
    @Schema(description = "사용자 소개", example = "안녕하세요, 저는 테스터입니다.", nullable = true)
    val bio: String? = null,
    @Schema(description = "사용자 프로필 이미지 URL", example = "https://example.com/profile.png", nullable = true)
    val profileImageUrl: String? = null,
)

data class ProfilePatchResponse(
    @Schema(description = "사용자 id", example = "1")
    val userId: Long,
    @Schema(description = "사용자 이메일", example = "user@example.com")
    val email: String,
    @Schema(description = "사용자 닉네임", example = "tester")
    val nickname: String?,
    @Schema(description = "사용자 이름", example = "홍길동")
    val name: String?,
    @Schema(description = "사용자 소개", example = "안녕하세요, 저는 테스터입니다.", nullable = true)
    val bio: String?,
    @Schema(description = "사용자 프로필 이미지 URL", example = "https://example.com/profile.png", nullable = true)
    val profileImageUrl: String?,
    @Schema(description = "사용자 권한", example = "USER")
    val role: String,
)
