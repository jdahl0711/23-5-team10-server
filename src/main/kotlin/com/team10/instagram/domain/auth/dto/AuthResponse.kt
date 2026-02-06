package com.team10.instagram.domain.auth.dto

import io.swagger.v3.oas.annotations.media.Schema

class AuthResponse {
    data class LoginResponse(
        @Schema(description = "액세스 토큰", example = "")
        val accessToken: String,
        // @Schema(description = "재발급 토큰", example = "")
        // val refreshToken: String,
    )

    data class RegisterResponse(
        @Schema(description = "회원가입 직후 로그인 상태 유지하기 위한 액세스 토큰", example = "")
        val accessToken: String,
        // @Schema(description = "재발급 토큰", example = "")
        // val refreshToken: String,
    )

    data class RefreshResponse(
        val accessToken: String,
        // val refreshToken: String,
    )

    data class CheckAccountResponse(
        val sentEmail: String,
    )

    data class CheckNicknameResponse(
        @Schema(description = "닉네임 사용 가능 여부", example = "true")
        val isAvailable: Boolean,
    )
}
