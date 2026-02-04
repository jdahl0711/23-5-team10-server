package com.team10.instagram.domain.auth.repository

import com.team10.instagram.domain.auth.model.RefreshToken
import org.springframework.data.repository.CrudRepository

interface RefreshTokenRepository : CrudRepository<RefreshToken, Long> {
    fun findByToken(token: String): RefreshToken?

    fun deleteByUserId(userId: Long)

    fun findByUserId(userId: Long): List<RefreshToken>
}
