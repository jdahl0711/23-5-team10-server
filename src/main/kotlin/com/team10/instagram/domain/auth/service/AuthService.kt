package com.team10.instagram.domain.auth.service

import com.team10.instagram.domain.auth.dto.AuthResponse.CheckAccountResponse
import com.team10.instagram.domain.auth.dto.AuthResponse.CheckNicknameResponse
import com.team10.instagram.domain.auth.jwt.JwtTokenProvider
import com.team10.instagram.domain.auth.model.RefreshToken
import com.team10.instagram.domain.auth.repository.RefreshTokenRepository
import com.team10.instagram.domain.user.Role
import com.team10.instagram.domain.user.model.User
import com.team10.instagram.domain.user.repository.UserRepository
import com.team10.instagram.global.error.CustomException
import com.team10.instagram.global.error.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId

@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
    private val passwordEncoder: PasswordEncoder,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenBlacklistService: JwtTokenBlacklistService,
) {
    private val maxNicknameLength = 30

    fun register(
        email: String,
        password: String,
        nickname: String,
    ) {
        if (userRepository.existsByEmail(email)) {
            throw CustomException(ErrorCode.EMAIL_ALREADY_EXISTS)
        }
        if (userRepository.existsByNickname(nickname)) {
            throw CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS)
        }
        if (!isValidNicknameFormat(nickname)) {
            throw CustomException(ErrorCode.INVALID_NICKNAME_FORMAT)
        }

        userRepository.save(
            User(
                email = email,
                password = passwordEncoder.encode(password),
                nickname = nickname,
                role = Role.USER,
                provider = null,
                providerId = null,
            ),
        )
    }

    fun login(
        loginId: String,
        password: String,
        response: HttpServletResponse,
    ): String {
        val user =
            findUserByLoginId(loginId)
                ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        if (!passwordEncoder.matches(password, user.password)) {
            throw CustomException(ErrorCode.INVALID_PASSWORD)
        }

        val accessToken = issueTokensAndGetAccessToken(user.userId!!, response)
        return accessToken
    }

    fun refresh(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): String {
        val refreshToken =
            jwtTokenProvider.extractRefreshTokenFromCookie(request)
                ?: throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)

        val savedToken =
            refreshTokenRepository.findByToken(refreshToken)
                ?: throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)

        if (savedToken.usedAt != null) {
            // 해당 유저 모든 토큰 폐기
            refreshTokenRepository.deleteByUserId(savedToken.userId)
            throw CustomException(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED)
        }

        if (jwtTokenProvider.isExpired(refreshToken)) {
            refreshTokenRepository.delete(savedToken)
            throw CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED)
        }

        savedToken.usedAt = LocalDateTime.now()
        refreshTokenRepository.save(savedToken)

        return issueTokensAndGetAccessToken(savedToken.userId, response)
    }

    private fun findUserByLoginId(loginId: String): User? =
        if (loginId.contains("@")) {
            userRepository.findByEmail(loginId)
        } else {
            userRepository.findByNickname(loginId)
        }

    fun logout(
        userId: Long,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val accessToken = jwtTokenProvider.resolveAccessToken(request)
        if (accessToken != null) jwtTokenBlacklistService.add(accessToken)
        refreshTokenRepository.deleteByUserId(userId)
        deleteAuthCookies(response)
    }

    @Transactional
    fun withdraw(
        userId: Long,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val accessToken = jwtTokenProvider.resolveAccessToken(request)
        if (accessToken != null) {
            jwtTokenBlacklistService.add(accessToken)
        }

        refreshTokenRepository.deleteByUserId(userId)
        deleteAuthCookies(response)
        userRepository.deleteByUserId(userId)
    }

    private fun issueTokensAndGetAccessToken(
        userId: Long,
        response: HttpServletResponse,
    ): String {
        // 1개의 기기에서만 로그인 가능하도록 설정 -> 추후 수정 가능
        refreshTokenRepository.deleteByUserId(userId)
        val accessToken = jwtTokenProvider.createAccessToken(userId)
        val refreshToken = jwtTokenProvider.createRefreshToken(userId)
        refreshTokenRepository.save(
            RefreshToken(
                userId = userId,
                token = refreshToken,
                expiresAt =
                    jwtTokenProvider
                        .getExpiration(refreshToken)
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime(),
            ),
        )

        // val accessMaxAge = jwtTokenProvider.accessTokenExpirationInMs / 1000
        val refreshMaxAge = jwtTokenProvider.refreshTokenExpirationInMs / 1000

        response.addHeader(
            "Set-Cookie",
            "refreshToken=$refreshToken; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=$refreshMaxAge; ",
        )

        return accessToken
    }

    private fun deleteAuthCookies(response: HttpServletResponse) {
        /*
        response.addHeader(
            "Set-Cookie",
            "accessToken=; Max-Age=0; HttpOnly; Secure; SameSite=None; Path=/",
        )*/
        response.addHeader(
            "Set-Cookie",
            "refreshToken=; Max-Age=0; HttpOnly; Secure; SameSite=None; Path=/",
        )
    }

    fun loginOAuth(
        email: String,
        nickname: String?,
        provider: String,
        providerId: String,
    ): String {
        val user =
            userRepository.findByEmail(email)
                ?: userRepository.save(
                    User(
                        email = email,
                        password = null,
                        nickname = if (nickname.isNullOrBlank()) "nickname" else nickname,
                        role = Role.USER,
                        provider = provider,
                        providerId = providerId,
                    ),
                )

        return jwtTokenProvider.createAccessToken(user.userId!!)
    }

    fun checkAccount(identity: String): CheckAccountResponse {
        val email =
            when {
                identity.contains("@") -> {
                    userRepository
                        .findByEmail(identity)
                        ?.email
                        ?: throw CustomException(ErrorCode.ACCOUNT_NOT_FOUND)
                }

                else -> {
                    userRepository.findEmailByNickname(identity)
                        ?: throw CustomException(ErrorCode.ACCOUNT_NOT_FOUND)
                }
            }

        // 이메일 전송
        // if(이메일 전송 실패) throw CustomException(ErrorCode.EMAIL_SEND_FAILED, mapOf("isExist" to true, "isSent" to false))

        val sentEmail = maskEmail(email)
        return CheckAccountResponse(sentEmail)
    }

    private fun maskEmail(email: String): String {
        val visibleCount = 1
        val parts = email.split("@")
        val local = parts[0]
        val domain = parts[1]

        val visible = local.take(visibleCount)
        val masked = "*".repeat(local.length - visibleCount)

        return "$visible$masked@$domain"
    }

    fun checkNickname(nickname: String): CheckNicknameResponse {
        if (userRepository.existsByEmail(nickname)) {
            throw CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS, mapOf("isAvailable" to false))
        }

        if (!isValidNicknameFormat(nickname)) {
            throw CustomException(ErrorCode.INVALID_NICKNAME_FORMAT)
        }

        return CheckNicknameResponse(isAvailable = true)
    }

    private fun isValidNicknameFormat(nickname: String): Boolean {
        val regex = Regex("^[a-z0-9_.]{1,$maxNicknameLength}$")
        return nickname.matches(regex)
    }

    fun getCurrentRefreshToken(userId: Long): String =
        refreshTokenRepository
            .findByUserId(userId)
            .firstOrNull()
            ?.token
            ?: throw CustomException(ErrorCode.INVALID_REFRESH_TOKEN)
}
