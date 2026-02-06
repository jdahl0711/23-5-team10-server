package com.team10.instagram.domain.auth.jwt

import com.team10.instagram.domain.auth.service.JwtTokenBlacklistService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}")
    private val secretKey: String,
    @Value("\${jwt.access-token-expiration-in-ms}")
    val accessTokenExpirationInMs: Long,
    @Value("\${jwt.refresh-token-expiration-in-ms}")
    val refreshTokenExpirationInMs: Long,
) {
    private val key = Keys.hmacShaKeyFor(secretKey.toByteArray())

    @Value("\${jwt.test-token}")
    private lateinit var testToken: String

    fun createAccessToken(userId: Long): String =
        createToken(
            userId = userId,
            expiresIn = accessTokenExpirationInMs,
            claims = mapOf("type" to "access"),
        )

    fun createRefreshToken(userId: Long): String =
        createToken(
            userId = userId,
            expiresIn = refreshTokenExpirationInMs,
            claims = mapOf("type" to "refresh"),
        )

    private fun createToken(
        userId: Long,
        expiresIn: Long,
        claims: Map<String, Any>?,
    ): String {
        val now = Date()
        val validity = Date(now.time + expiresIn)
        val tokenClaims = claims.orEmpty() + mapOf("jti" to UUID.randomUUID().toString())

        val token =
            Jwts
                .builder()
                .setSubject(userId.toString())
                .addClaims(tokenClaims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact()
        println("Token Created: {$token}")

        return token
    }

    fun getUserId(token: String): Long =
        Jwts
            .parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
            .subject
            .toLong()

    fun getExpiration(token: String): Date =
        Jwts
            .parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
            .expiration

    fun isExpired(token: String): Boolean =
        try {
            getExpiration(token).before(Date())
        } catch (e: Exception) {
            true
        }

    fun validateToken(
        token: String,
        jwtTokenBlacklistService: JwtTokenBlacklistService,
    ): Boolean {
        if (jwtTokenBlacklistService.contains(token)) return false

        if (token == testToken) return true

        return try {
            Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun resolveAccessToken(request: HttpServletRequest): String? {
        val authorizationHeader = request.getHeader("Authorization")
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7)
        }

        return extractAccessTokenFromCookie(request)
    }

    fun extractAccessTokenFromCookie(request: HttpServletRequest): String? =
        request.cookies
            ?.firstOrNull { it.name == "accessToken" }
            ?.value

    fun extractRefreshTokenFromCookie(request: HttpServletRequest): String? =
        request.cookies
            ?.firstOrNull { it.name == "refreshToken" }
            ?.value
}
