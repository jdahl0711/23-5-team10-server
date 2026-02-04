package com.team10.instagram.domain.auth.jwt

import com.team10.instagram.domain.auth.model.RefreshToken
import com.team10.instagram.domain.auth.repository.RefreshTokenRepository
import com.team10.instagram.domain.auth.service.JwtTokenBlacklistService
import com.team10.instagram.domain.user.Role
import com.team10.instagram.domain.user.model.User
import com.team10.instagram.domain.user.repository.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.ZoneId

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtTokenBlacklistService: JwtTokenBlacklistService,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
) : OncePerRequestFilter() {
    @Value("\${jwt.test-token}")
    private lateinit var testToken: String

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = resolveToken(request)

        if (token != null) {
            if (token == testToken) {
                handleTestToken(request)
            } else if (jwtTokenProvider.validateToken(token!!, jwtTokenBlacklistService)) {
                // 실제 토큰 처리
                val userId = jwtTokenProvider.getUserId(token!!)
                request.setAttribute("userId", userId)

                val user = userRepository.findByUserId(userId)

                if (user != null) {
                    val auth =
                        UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            listOf(SimpleGrantedAuthority("ROLE_${user.role.name}")),
                        )
                    SecurityContextHolder.getContext().authentication = auth
                }
            }
        }

        // 토큰이 없거나 유효하지 않아도 통과시킴
        // CORS 요청, Public API, 에러 페이지 등은 SecurityConfig가 알아서 처리
        filterChain.doFilter(request, response)
    }

    private fun handleTestToken(request: HttpServletRequest) {
        var user = userRepository.findByEmail("test@swagger.com")

        if (user == null) {
            user = userRepository.save(
                User(
                    email = "test@swagger.com",
                    password = BCryptPasswordEncoder().encode("password123"),
                    nickname = "swagger_tester",
                    role = Role.USER,
                )
            )

            refreshTokenRepository.deleteByUserId(user.userId!!)
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
        }


        request.setAttribute("userId", user.userId)
        val auth =
            UsernamePasswordAuthenticationToken(
                user,
                null,
                listOf(SimpleGrantedAuthority("ROLE_USER")),
            )
        SecurityContextHolder.getContext().authentication = auth
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7)
        }
        return null
    }
}
