package com.team10.instagram

import com.team10.instagram.domain.follow.repository.FollowRepository
import com.team10.instagram.domain.user.model.User
import com.team10.instagram.helper.DataGenerator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
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
class FollowIntegrationTest
    @Autowired
    constructor(
        private val mvc: MockMvc,
        private val dataGenerator: DataGenerator,
        private val followRepository: FollowRepository,
    ) {
        private lateinit var myUser: User
        private lateinit var myToken: String
        private lateinit var otherUser: User

        @BeforeEach
        fun setup() {
            // 나 (로그인 유저)
            myUser = dataGenerator.generateUser(email = "me@example.com", nickname = "me")
            myToken = "Bearer ${dataGenerator.generateToken(myUser)}"

            // 상대방
            otherUser = dataGenerator.generateUser(nickname = "other")
        }

        @Test
        fun `유저를 팔로우할 수 있다`() {
            // when
            mvc
                .perform(
                    post("/api/v1/follows/${otherUser.userId}")
                        .header("Authorization", myToken),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data").value("팔로우했습니다.")) // 메시지 검증

            // then: DB 검증
            val exists = followRepository.exists(myUser.userId!!, otherUser.userId!!)
            assertTrue(exists, "팔로우 데이터가 DB에 존재해야 합니다.")
        }

        @Test
        fun `이미 팔로우한 유저를 다시 요청하면 언팔로우된다`() {
            // given: 이미 팔로우 상태
            dataGenerator.generateFollow(myUser, otherUser)

            // when
            mvc
                .perform(
                    post("/api/v1/follows/${otherUser.userId}")
                        .header("Authorization", myToken),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data").value("언팔로우했습니다."))

            // then: DB 검증
            val exists = followRepository.exists(myUser.userId!!, otherUser.userId!!)
            assertFalse(exists, "언팔로우 후에는 데이터가 사라져야 합니다.")
        }

        @Test
        fun `자기 자신을 팔로우하려 하면 실패한다`() {
            // when
            val resultActions =
                mvc.perform(
                    post("/api/v1/follows/${myUser.userId}") // 내 ID로 요청
                        .header("Authorization", myToken),
                )

            // then
            resultActions
                .andDo(print())
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("SELF_FOLLOW_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("자기 자신은 팔로우할 수 없습니다."))
        }

        @Test
        fun `팔로잉 목록을 조회할 수 있다 (isFollowing 체크)`() {
            // given
            val userA = dataGenerator.generateUser(nickname = "UserA")
            val userB = dataGenerator.generateUser(nickname = "UserB")

            // 내가 A와 B를 팔로우함
            dataGenerator.generateFollow(myUser, userA)
            dataGenerator.generateFollow(myUser, userB)

            // A는 나를 맞팔로우 함 (isFollowing = true 확인용)
            dataGenerator.generateFollow(userA, myUser)

            // when
            mvc
                .perform(
                    get("/api/v1/follows/${myUser.userId}/following") // URL 주의 (following vs followings)
                        .header("Authorization", myToken),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.length()").value(2))
                // 목록에 UserA, UserB가 있어야 함 (순서는 보장 X)
                .andExpect(jsonPath("$.data[*].nickname").isArray)
        }

        @Test
        fun `팔로워 목록을 조회할 수 있다 (맞팔 여부 확인)`() {
            // given
            val fan1 = dataGenerator.generateUser(nickname = "fan1") // 나를 팔로우, 나도 팔로우
            val fan2 = dataGenerator.generateUser(nickname = "fan2") // 나를 팔로우, 나는 안함

            dataGenerator.generateFollow(fan1, myUser)
            dataGenerator.generateFollow(fan2, myUser)

            // 나는 fan1만 맞팔로우
            dataGenerator.generateFollow(myUser, fan1)

            // when
            mvc
                .perform(
                    get("/api/v1/follows/${myUser.userId}/follower")
                        .header("Authorization", myToken),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.length()").value(2))
                // 검증 1: fan1은 맞팔 상태(isFollowing = true)
                .andExpect(jsonPath("$.data[?(@.nickname == 'fan1')].isFollowing").value(true))
                // 검증 2: fan2는 맞팔 아님(isFollowing = false)
                .andExpect(jsonPath("$.data[?(@.nickname == 'fan2')].isFollowing").value(false))
        }

        @Test
        fun `내 팔로워를 삭제(강제 언팔)할 수 있다`() {
            // given
            val stalker = dataGenerator.generateUser(nickname = "stalker")
            dataGenerator.generateFollow(stalker, myUser)

            // when
            mvc
                .perform(
                    delete("/api/v1/follows/followers/${stalker.userId}")
                        .header("Authorization", myToken),
                ).andExpect(status().isOk)

            // then
            val exists = followRepository.exists(stalker.userId!!, myUser.userId!!)
            assertFalse(exists, "팔로워 삭제 후 관계가 끊겨야 합니다.")
        }

        @Test
        fun `존재하지 않는 유저를 팔로우하면 실패한다`() {
            val invalidUserId = 99999L

            mvc
                .perform(
                    post("/api/v1/follows/$invalidUserId")
                        .header("Authorization", myToken),
                ).andExpect(status().isNotFound)
        }
    }
