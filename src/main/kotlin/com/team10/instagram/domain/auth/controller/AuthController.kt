package com.team10.instagram.domain.auth.controller

import com.team10.instagram.domain.auth.dto.AuthRequest.CheckAccountRequest
import com.team10.instagram.domain.auth.dto.AuthRequest.CheckNicknameRequest
import com.team10.instagram.domain.auth.dto.AuthRequest.LoginRequest
import com.team10.instagram.domain.auth.dto.AuthRequest.RefreshRequest
import com.team10.instagram.domain.auth.dto.AuthRequest.RegisterRequest
import com.team10.instagram.domain.auth.dto.AuthResponse.CheckAccountResponse
import com.team10.instagram.domain.auth.dto.AuthResponse.CheckNicknameResponse
import com.team10.instagram.domain.auth.dto.AuthResponse.LoginResponse
import com.team10.instagram.domain.auth.dto.AuthResponse.RefreshResponse
import com.team10.instagram.domain.auth.dto.AuthResponse.RegisterResponse
import com.team10.instagram.domain.auth.service.AuthService
import com.team10.instagram.domain.auth.service.JwtTokenBlacklistService
import com.team10.instagram.domain.user.LoggedInUser
import com.team10.instagram.domain.user.model.User
import com.team10.instagram.global.common.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val jwtTokenBlacklistService: JwtTokenBlacklistService,
) {
    @Operation(summary = "로그인", description = "이메일 또는 닉네임과 비밀번호로 로그인합니다")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "로그인 성공"),
            SwaggerApiResponse(responseCode = "401", description = "비밀번호가 틀린 경우"),
            SwaggerApiResponse(responseCode = "404", description = "사용자를 찾을 수 없는 경우"),
        ],
    )
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
    ): ApiResponse<LoginResponse> {
        val tokenPair = authService.login(request.loginId, request.password)
        return ApiResponse.onSuccess(tokenPair)
    }

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 닉네임으로 회원가입 후 자동 로그인")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "회원가입 및 로그인 성공"),
            SwaggerApiResponse(responseCode = "400", description = "이미 존재하는 이메일/닉네임"),
        ],
    )
    @PostMapping("/register")
    fun register(
        @Valid @RequestBody request: RegisterRequest,
    ): ApiResponse<RegisterResponse> {
        authService.register(request.email, request.password, request.nickname)
        val tokenPair = authService.login(request.email, request.password)
        return ApiResponse.onSuccess(RegisterResponse(accessToken = tokenPair.accessToken, refreshToken = tokenPair.refreshToken))
    }

    @Operation(summary = "액세스 토큰 재발급", description = "재발급 토큰을 인증하여 액세스 토큰 재발급")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "액세스 토큰 재발급 성공"),
            SwaggerApiResponse(responseCode = "401", description = "유효하지 않거나 만료되거나 이미 한 번 사용된 재발급 토큰"),
        ],
    )
    @PostMapping("/refresh")
    fun refresh(
        @Valid @RequestBody request: RefreshRequest,
    ): ApiResponse<RefreshResponse> {
        val refreshResponse = authService.refresh(request.refreshToken)
        return ApiResponse.onSuccess(refreshResponse)
    }

    @Operation(summary = "로그아웃", description = "현재 JWT Access Token을 무효화합니다")
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "로그아웃 성공"),
        ],
    )
    @PostMapping("/logout")
    fun logout(
        @RequestHeader("Authorization") authorizationHeader: String,
    ): ApiResponse<String> {
        val accessToken = authorizationHeader.replace("Bearer", "").trim()
        authService.logout(accessToken)
        return ApiResponse.onSuccess("Logged out successfully")
    }

    @PostMapping("/check-account")
    fun checkAccount(
        @Valid @RequestBody request: CheckAccountRequest,
    ): ApiResponse<CheckAccountResponse> {
        val checkAccountResponse = authService.checkAccount(request.identity)
        return ApiResponse.onSuccess(checkAccountResponse)
    }

    @GetMapping("/check-nickname")
    fun checkNickname(
        @Valid @RequestBody request: CheckNicknameRequest,
    ): ApiResponse<CheckNicknameResponse> {
        val checkNickNameResponse = authService.checkNickname(request.nickname)
        return ApiResponse.onSuccess(checkNickNameResponse)
    }

    @GetMapping("/current-refresh-token")
    fun getCurrentRefreshToken(
        @Parameter(hidden = true) @LoggedInUser user: User,
    ): ApiResponse<String> = ApiResponse.onSuccess(authService.getCurrentRefreshToken(user.userId!!))

/*
    // TODO
    @PostMapping("/oauth")
    fun oauthLogin(
        @RequestBody request: OAuthLoginRequest,
    ): ApiResponse<LoginResponse> {
        val token =
            authService.loginOAuth(
                email = request.email,
                nickname = request.nickname,
                provider = request.provider,
                providerId = request.providerId,
            )
        return ApiResponse.onSuccess(LoginResponse(token, token))
    }*/
}
