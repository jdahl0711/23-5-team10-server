package com.team10.instagram.domain.auth.service

import com.team10.instagram.domain.auth.model.CustomOAuth2User
import com.team10.instagram.domain.user.model.User
import com.team10.instagram.domain.user.repository.UserRepository
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2UserService(
    private val userRepository: UserRepository,
) : OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val delegate = DefaultOAuth2UserService()
        val oAuth2User = delegate.loadUser(userRequest)

        val attributes = oAuth2User.attributes
        val kakaoAccount = attributes["kakao_account"] as Map<*, *>
        val profile = kakaoAccount["profile"] as Map<*, *>

        val email = kakaoAccount["email"] as String
        val nickname = profile["nickname"] as String

        val user =
            userRepository.findByEmail(email)
                ?: userRepository.save(
                    User(
                        email = email,
                        nickname = nickname,
                        provider = "KAKAO",
                    ),
                )

        return CustomOAuth2User(user, attributes)
    }
}
