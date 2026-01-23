package com.team10.instagram.domain.auth.jwt

import com.team10.instagram.domain.auth.model.CustomOAuth2User
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2LoginSuccessHandler(
    private val jwtTokenProvider: JwtTokenProvider,
) : AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val user = (authentication.principal as CustomOAuth2User).user
        val accessToken = jwtTokenProvider.createAccessToken(user.userId!!)
        val refreshToken = jwtTokenProvider.createRefreshToken(user.userId!!)

        response.addCookie(
            Cookie("refreshToken", refreshToken).apply {
                isHttpOnly = true
                secure = true
                path = "/"
            },
        )
        response.sendRedirect("http://localhost:8080/oauth?accessToken=$accessToken")
    }
}
