package com.team10.instagram.domain.auth.service

import com.team10.instagram.domain.auth.dto.AuthResponse.CheckAccountResponse
import com.team10.instagram.domain.auth.dto.AuthResponse.CheckNicknameResponse
import com.team10.instagram.domain.auth.dto.AuthResponse.LoginResponse
import com.team10.instagram.domain.auth.dto.AuthResponse.RefreshResponse
import com.team10.instagram.domain.auth.jwt.JwtTokenProvider
import com.team10.instagram.domain.auth.model.RefreshToken
import com.team10.instagram.domain.auth.repository.RefreshTokenRepository
import com.team10.instagram.domain.user.Role
import com.team10.instagram.domain.user.model.User
import com.team10.instagram.domain.user.repository.UserRepository
import com.team10.instagram.global.error.CustomException
import com.team10.instagram.global.error.ErrorCode
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
    ): LoginResponse {
        val user =
            findUserByLoginId(loginId)
                ?: throw CustomException(ErrorCode.USER_NOT_FOUND)

        if (!passwordEncoder.matches(password, user.password)) {
            throw CustomException(ErrorCode.INVALID_PASSWORD)
        }
        // 1개의 기기에서만 로그인 가능하도록 설정 -> 추후 수정 가능
        refreshTokenRepository.deleteByUserId(user.userId!!)
        val accessToken = jwtTokenProvider.createAccessToken(user.userId!!)
        val refreshToken = jwtTokenProvider.createRefreshToken(user.userId!!)
        refreshTokenRepository.save(
            RefreshToken(
                userId = user.userId!!,
                token = refreshToken,
                expiresAt =
                    jwtTokenProvider
                        .getExpiration(refreshToken)
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime(),
            ),
        )

        return LoginResponse(accessToken, refreshToken)
    }

    fun refresh(refreshToken: String): RefreshResponse {
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

        val newAccessToken = jwtTokenProvider.createAccessToken(savedToken.userId)
        val newRefreshToken = jwtTokenProvider.createRefreshToken(savedToken.userId)
        refreshTokenRepository.save(
            RefreshToken(
                userId = savedToken.userId,
                token = newRefreshToken,
                expiresAt =
                    jwtTokenProvider
                        .getExpiration(newRefreshToken)
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime(),
            ),
        )

        return RefreshResponse(newAccessToken, newRefreshToken)
    }

    private fun findUserByLoginId(loginId: String): User? =
        if (loginId.contains("@")) {
            userRepository.findByEmail(loginId)
        } else {
            userRepository.findByNickname(loginId)
        }

    fun logout(accessToken: String) {
        jwtTokenBlacklistService.add(accessToken)
        val userId = jwtTokenProvider.getUserId(accessToken)
        refreshTokenRepository.deleteByUserId(userId)
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

    fun getCurrentRefreshToken(userId: Long): String = refreshTokenRepository.findByUserId(userId)[0].token
}
