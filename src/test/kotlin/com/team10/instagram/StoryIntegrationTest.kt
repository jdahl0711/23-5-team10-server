package com.team10.instagram

import com.fasterxml.jackson.databind.ObjectMapper
import com.team10.instagram.domain.follow.repository.FollowRepository
import com.team10.instagram.domain.story.dto.StoryCreateRequest
import com.team10.instagram.domain.story.repository.StoryRepository
import com.team10.instagram.domain.user.model.User
import com.team10.instagram.helper.DataGenerator
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StoryIntegrationTest
    @Autowired
    constructor(
        private val mvc: MockMvc,
        private val dataGenerator: DataGenerator,
        private val storyRepository: StoryRepository,
        private val followRepository: FollowRepository,
    ) {
        private val objectMapper = ObjectMapper()
        private lateinit var myUser: User
        private lateinit var myToken: String

        @BeforeEach
        fun setup() {
            myUser = dataGenerator.generateUser(email = "me@example.com", nickname = "me")
            myToken = "Bearer ${dataGenerator.generateToken(myUser)}"
        }

        @Test
        fun `스토리를 업로드할 수 있다`() {
            // given
            val request = StoryCreateRequest(imageUrl = "https://new-story.jpg")

            // when & then
            mvc
                .perform(
                    post("/api/v1/stories")
                        .header("Authorization", myToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.isSuccess").value(true))
        }

        @Test
        fun `이미지 URL이 비어있으면 업로드에 실패한다`() {
            // given
            val request = StoryCreateRequest(imageUrl = "")

            // when & then
            mvc
                .perform(
                    post("/api/v1/stories")
                        .header("Authorization", myToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andDo(print())
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
        }

        @Test
        fun `피드에는 내가 팔로우한 사람의 24시간 이내 스토리만 보인다`() {
            // given
            // 1. 유저 준비
            val friend = dataGenerator.generateUser(nickname = "friend")
            val stranger = dataGenerator.generateUser(nickname = "stranger")

            // 2. 관계 설정: 나는 친구를 팔로우함
            dataGenerator.generateFollow(myUser, friend)

            // 3. 스토리 생성
            dataGenerator.generateStory(friend) // 친구 스토리 (보여야 함)
            dataGenerator.generateStory(stranger) // 남의 스토리 (안 보여야 함)

            // when & then
            mvc
                .perform(
                    get("/api/v1/stories/feed")
                        .header("Authorization", myToken),
                ).andDo(print())
                .andExpect(status().isOk)
                // 친구는 보이고
                .andExpect(jsonPath("$.data[?(@.nickname == '${friend.nickname}')]").exists())
                // 남은 안 보여야 함
                .andExpect(jsonPath("$.data[?(@.nickname == '${stranger.nickname}')]").doesNotExist())
                // 아직 안 봤으므로 테두리는 true
                .andExpect(jsonPath("$.data[0].hasUnseenStory").value(true))
        }

        @Test
        fun `타인의 스토리를 조회하면 조회수는 가려지고(null), 읽음 처리된다`() {
            // given
            val friend = dataGenerator.generateUser(nickname = "view_friend")
            dataGenerator.generateStory(friend)

            // when & then
            mvc
                .perform(
                    get("/api/v1/stories/user/${friend.userId}")
                        .header("Authorization", myToken),
                ).andDo(print())
                .andExpect(status().isOk)
                // 남의 스토리니까 viewCount는 null
                .andExpect(jsonPath("$.data[0].viewCount").isEmpty)
        }

        @Test
        fun `내 스토리를 조회하면 조회수가 보인다`() {
            // given
            dataGenerator.generateStory(myUser)

            // when & then
            mvc
                .perform(
                    get("/api/v1/stories/user/${myUser.userId}")
                        .header("Authorization", myToken),
                ).andDo(print())
                .andExpect(status().isOk)
                // 내 스토리니까 숫자가 나와야 함 (0)
                .andExpect(jsonPath("$.data[0].viewCount").isNumber)
        }

        @Test
        fun `존재하지 않는 유저의 스토리를 조회하면 실패한다`() {
            // given
            val wrongUserId = 99999L

            // when & then
            mvc
                .perform(
                    get("/api/v1/stories/user/$wrongUserId")
                        .header("Authorization", myToken),
                ).andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
        }

        @Test
        fun `내 스토리를 삭제할 수 있다`() {
            // given
            dataGenerator.generateStory(myUser)
            // 삭제할 ID 조회
            val storyId = storyRepository.findAllByUserId(myUser.userId!!)[0].id

            // when & then
            mvc
                .perform(
                    delete("/api/v1/stories/$storyId")
                        .header("Authorization", myToken),
                ).andExpect(status().isOk)

            // DB 검증
            assertNull(storyRepository.findOwnerId(storyId))
        }

        @Test
        fun `타인의 스토리를 삭제하려고 하면 실패한다`() {
            // given
            val otherUser = dataGenerator.generateUser(nickname = "other")
            dataGenerator.generateStory(otherUser)

            val targetStoryId = storyRepository.findAllByUserId(otherUser.userId!!)[0].id

            // when & then
            mvc
                .perform(
                    delete("/api/v1/stories/$targetStoryId")
                        .header("Authorization", myToken),
                ).andDo(print())
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.code").value("STORY_NOT_OWNER"))
        }

        @Test
        fun `내 스토리가 존재하면 피드 목록의 맨 처음에 포함된다`() {
            // given
            // 1. 친구 생성 및 팔로우
            val friend = dataGenerator.generateUser(nickname = "friend")
            dataGenerator.generateFollow(myUser, friend)

            // 2. 스토리 생성 (나 & 친구 둘 다 생성)
            dataGenerator.generateStory(myUser) // 내 스토리
            dataGenerator.generateStory(friend) // 친구 스토리

            // when & then
            mvc
                .perform(
                    get("/api/v1/stories/feed")
                        .header("Authorization", myToken),
                ).andDo(print())
                .andExpect(status().isOk)
                // 검증 1: 총 2명이 조회되어야 함
                .andExpect(jsonPath("$.data.length()").value(2))
                // 검증 2: 첫 번째(index 0)는 무조건 '나'여야 함
                .andExpect(jsonPath("$.data[0].nickname").value(myUser.nickname))
                // 검증 3: 두 번째(index 1)는 '친구'여야 함
                .andExpect(jsonPath("$.data[1].nickname").value(friend.nickname))
        }
    }
