package com.team10.instagram.domain.auth.jwt

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
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtTokenBlacklistService: JwtTokenBlacklistService,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {
    private val pathMatcher = AntPathMatcher()

    @Value("\${jwt.test-token}")
    private lateinit var testToken: String

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (isPublicPath(request.requestURI)) {
            filterChain.doFilter(request, response)
            return
        }

        val token = resolveToken(request)

        if (token == testToken) {
            val user =
                userRepository.findByEmail("test@swagger.com")
                    ?: userRepository.save(
                        User(
                            email = "test@swagger.com",
                            password = BCryptPasswordEncoder().encode("password123"),
                            nickname = "swagger_tester",
                            role = Role.USER,
                        ),
                    )
            request.setAttribute("userId", user.userId)
            val auth =
                UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_USER")),
                )
            SecurityContextHolder.getContext().authentication = auth
            filterChain.doFilter(request, response)
            return
        } else if (token != null && jwtTokenProvider.validateToken(token, jwtTokenBlacklistService)) {
            val userId = jwtTokenProvider.getUserId(token)
            request.setAttribute("userId", userId)
            val user = userRepository.findById(userId).orElse(null)
            if (user != null) {
                val auth =
                    UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        listOf(SimpleGrantedAuthority("ROLE_${user.role.name}")),
                    )
                SecurityContextHolder.getContext().authentication = auth
            }
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing token")
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7)
        }
        return null
    }

    private fun isPublicPath(path: String): Boolean =
        pathMatcher.match("/api/v1/auth/**", path) ||
            pathMatcher.match("/swagger-ui/**", path) ||
            pathMatcher.match("/v3/api-docs/**", path) ||
            pathMatcher.match("/actuator/health", path) ||
            pathMatcher.match("/oauth2/**", path) ||
            pathMatcher.match("/login/**", path)
}
