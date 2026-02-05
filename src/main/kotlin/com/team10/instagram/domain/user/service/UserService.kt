package com.team10.instagram.domain.user.service

import com.team10.instagram.domain.follow.repository.FollowRepository
import com.team10.instagram.domain.post.repository.PostRepository
import com.team10.instagram.domain.user.dto.ProfilePatchResponse
import com.team10.instagram.domain.user.dto.ProfileResponse
import com.team10.instagram.domain.user.dto.UserSearchResponse
import com.team10.instagram.domain.user.dto.UserSearchResponseDtoUnit
import com.team10.instagram.domain.user.model.User
import com.team10.instagram.domain.user.repository.UserRepository
import com.team10.instagram.global.error.CustomException
import com.team10.instagram.global.error.ErrorCode
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val followRepository: FollowRepository,
) {
    fun deleteUser(userId: Long) {
        userRepository.deleteByUserId(userId)
    }

    fun search(
        loggedInUserId: Long,
        q: String,
    ): UserSearchResponse {
        val users = userRepository.findByNicknameContainingIgnoreCaseOrNameContainingIgnoreCase(q, q)
        val userDtos: List<UserSearchResponseDtoUnit> =
            users.map { user ->
                UserSearchResponseDtoUnit(
                    userId = user.userId!!,
                    nickname = user.nickname,
                    profileImageUrl = user.profileImageUrl,
                    name = user.name,
                    isFollowed = followRepository.exists(loggedInUserId, user.userId!!),
                )
            }
        return UserSearchResponse(userDtos)
    }

    fun getProfile(
        userId: Long,
        loggedInUser: User,
    ): ProfileResponse {
        val profileUser =
            try {
                userRepository.findByUserId(userId)!!
            } catch (e: Exception) {
                throw CustomException(ErrorCode.USER_NOT_FOUND)
            }

        return ProfileResponse(
            userId = profileUser.userId!!,
            nickname = profileUser.nickname,
            name = profileUser.name,
            bio = profileUser.bio,
            profileImageUrl = profileUser.profileImageUrl,
            postCount = postRepository.countByUserId(profileUser.userId!!),
            followerCount = followRepository.countFollowers(profileUser.userId!!),
            followingCount = followRepository.countFollowings(profileUser.userId!!),
            isMe = profileUser.userId == loggedInUser.userId,
            isFollowed = followRepository.exists(loggedInUser.userId!!, profileUser.userId!!),
        )
    }

    fun patchProfile(
        userId: Long,
        nickname: String?,
        name: String?,
        bio: String?,
        profileImageUrl: String?,
    ): ProfilePatchResponse {
        val profileUser =
            try {
                userRepository.findByUserId(userId)!!
            } catch (e: Exception) {
                throw CustomException(ErrorCode.USER_NOT_FOUND)
            }

        if (nickname != null) {
            profileUser.nickname = nickname
        }
        if (name != null) {
            profileUser.name = name
        }
        if (bio != null) {
            profileUser.bio = bio
        }
        if (profileImageUrl != null) {
            profileUser.profileImageUrl = profileImageUrl
        }

        userRepository.save(profileUser)

        return ProfilePatchResponse(
            userId = profileUser.userId!!,
            email = profileUser.email,
            nickname = profileUser.nickname,
            name = profileUser.name,
            bio = profileUser.bio,
            profileImageUrl = profileUser.profileImageUrl,
            role = profileUser.role.name,
        )
    }
}
