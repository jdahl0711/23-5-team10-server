package com.team10.instagram.domain.user.model

import com.team10.instagram.domain.user.Role
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("users")
data class User(
    @Id val userId: Long? = null,
    val email: String,
    var password: String? = null,
    var nickname: String,
    var name: String? = null,
    var profileImageUrl: String? = null,
    var bio: String? = null,
    var role: Role = Role.USER,
    var provider: String? = null,
    var providerId: String? = null,
    @CreatedDate val createdAt: LocalDateTime = LocalDateTime.now(),
    @LastModifiedDate var updatedAt: LocalDateTime = LocalDateTime.now(),
)
