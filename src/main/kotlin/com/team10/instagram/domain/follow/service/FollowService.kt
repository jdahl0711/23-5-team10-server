package com.team10.instagram.domain.follow.service

import com.team10.instagram.domain.follow.dto.FollowResponse
import com.team10.instagram.domain.follow.repository.FollowRepository
import com.team10.instagram.domain.user.repository.UserRepository
import com.team10.instagram.global.error.CustomException
import com.team10.instagram.global.error.ErrorCode
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FollowService(
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository,
) {
    // 1. 팔로우 <-> 언팔로우 전환
    @Transactional
    fun toggleFollow(
        fromUserId: Long,
        toUserId: Long,
    ): String {
        if (fromUserId == toUserId) throw CustomException(ErrorCode.SELF_FOLLOW_NOT_ALLOWED)

        if (!userRepository.existsByUserId(toUserId)) {
            throw CustomException(ErrorCode.USER_NOT_FOUND)
        }
        if (followRepository.exists(fromUserId, toUserId)) {
            followRepository.delete(fromUserId, toUserId)
            return "언팔로우했습니다."
        }

        // 팔로우 안 했으면 팔로우
        try {
            followRepository.save(fromUserId, toUserId)
            return "팔로우했습니다."
        } catch (e: DuplicateKeyException) {
            // 동시에 요청이 들어와서 중복 저장 시도
            throw CustomException(ErrorCode.ALREADY_FOLLOWING)
        }
    }

    // 2. 팔로워 목록 조회
    fun getFollowers(
        targetUserId: Long,
        loginUserId: Long,
    ): List<FollowResponse> = followRepository.findAllFollowers(targetUserId, loginUserId)

    // 3. 팔로잉 목록 조회
    fun getFollowings(
        targetUserId: Long,
        loginUserId: Long,
    ): List<FollowResponse> = followRepository.findAllFollowings(targetUserId, loginUserId)

    // 4. 팔로워 삭제 (강제 언팔)
    @Transactional
    fun deleteFollower(
        myUserId: Long,
        followerId: Long,
    ) {
        if (!followRepository.exists(fromUserId = followerId, toUserId = myUserId)) {
            throw CustomException(ErrorCode.NOT_FOLLOWING)
        }
        // DELETE FROM follow WHERE from = follower AND to = me
        followRepository.delete(followerId, myUserId)
    }
}
