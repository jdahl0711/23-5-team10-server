package com.team10.instagram.domain.auth.jwt

import com.team10.instagram.domain.auth.model.CustomOAuth2User
import com.team10.instagram.domain.auth.model.RefreshToken
import com.team10.instagram.domain.auth.repository.RefreshTokenRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component
import java.time.ZoneId

@Component
class OAuth2LoginSuccessHandler(
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenRepository: RefreshTokenRepository,
) : AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val user = (authentication.principal as CustomOAuth2User).user

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

        val accessMaxAge = jwtTokenProvider.accessTokenExpirationInMs / 1000
        val refreshMaxAge = jwtTokenProvider.refreshTokenExpirationInMs / 1000

        /*
        response.addHeader(
            "Set-Cookie",
            "accessToken=$accessToken; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=$accessMaxAge; ",
        )*/
        response.addHeader(
            "Set-Cookie",
            "refreshToken=$refreshToken; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=$refreshMaxAge; ",
        )

        response.sendRedirect("https://d1ki8kre4wetjx.cloudfront.net/oauth?accessToken=$accessToken")
    }
}
