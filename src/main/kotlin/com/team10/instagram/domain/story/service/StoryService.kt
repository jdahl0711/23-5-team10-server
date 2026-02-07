package com.team10.instagram.domain.story.service

import com.team10.instagram.domain.story.dto.StoryCreateRequest
import com.team10.instagram.domain.story.dto.StoryDetailResponse
import com.team10.instagram.domain.story.dto.StoryFeedResponse
import com.team10.instagram.domain.story.repository.StoryRepository
import com.team10.instagram.domain.user.repository.UserRepository
import com.team10.instagram.global.error.CustomException
import com.team10.instagram.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StoryService(
    private val storyRepository: StoryRepository,
    private val userRepository: UserRepository,
) {
    // 1. 스토리 업로드
    @Transactional
    fun createStory(
        userId: Long,
        request: StoryCreateRequest,
    ) {
        // (보통은 DTO의 @NotBlank로 처리합니다)
        storyRepository.save(userId, request.imageUrl)
    }

    // 2. 스토리 피드 (상단 바)
    @Transactional(readOnly = true)
    fun getStoryFeed(loginUserId: Long): List<StoryFeedResponse> {
        // 1. 내 스토리가 존재하는지 확인하고 가져오기
        val myFeedItem = storyRepository.findMyStoryFeedItem(loginUserId)

        // 2. 팔로우한 친구들의 스토리 피드 가져오기
        val friendsFeedList = storyRepository.findStoryFeed(loginUserId)

        // 3. 리스트 합치기
        val combinedList =
            if (myFeedItem != null) {
                // 내 스토리가 있으면 맨 앞에 추가
                listOf(myFeedItem) + friendsFeedList
            } else {
                // 없으면 친구들 목록만 반환
                friendsFeedList
            }

        // 4. 각 유저별로 실제 스토리 목록(stories)을 채워넣기
        return combinedList.map { feedItem ->
            val stories = storyRepository.findAllByUserId(feedItem.userId)
            feedItem.copy(stories = stories)
        }
    }

    // 3. 스토리 상세 조회
    @Transactional
    fun getUserStories(
        loginUserId: Long,
        targetUserId: Long,
    ): List<StoryDetailResponse> {
        // 유저 존재 확인
        if (!userRepository.existsById(targetUserId)) {
            throw CustomException(ErrorCode.USER_NOT_FOUND)
        }

        val stories = storyRepository.findAllByUserId(targetUserId)

        // 타인의 스토리를 보는 경우
        if (loginUserId != targetUserId) {
            return stories.map { story ->
                // 조회했음을 DB에 기록
                storyRepository.saveView(loginUserId, story.id)

                // 조회수는 가려서 반환
                story.copy(viewCount = null)
            }
        }

        return stories
    }

    // 4. 스토리 삭제
    @Transactional
    fun deleteStory(
        userId: Long,
        storyId: Long,
    ) {
        val ownerId =
            storyRepository.findOwnerId(storyId)
                ?: throw CustomException(ErrorCode.STORY_NOT_FOUND)

        if (ownerId != userId) {
            throw CustomException(ErrorCode.STORY_NOT_OWNER)
        }

        storyRepository.delete(storyId)
    }
}
