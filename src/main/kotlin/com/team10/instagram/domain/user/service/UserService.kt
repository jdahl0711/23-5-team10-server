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
        request: Map<String, Any?>,
    ): ProfilePatchResponse {
        val profileUser =
            try {
                userRepository.findByUserId(userId)!!
            } catch (e: Exception) {
                throw CustomException(ErrorCode.USER_NOT_FOUND)
            }

        if (request.containsKey("nickname")) {
            profileUser.nickname = request["nickname"] as String
        }
        if (request.containsKey("name")) {
            profileUser.name = request["name"] as String?
        }
        if (request.containsKey("bio")) {
            profileUser.bio = request["bio"] as String?
        }
        if (request.containsKey("profileImageUrl")) {
            profileUser.profileImageUrl = request["profileImageUrl"] as String?
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
