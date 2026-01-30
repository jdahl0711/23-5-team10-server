package com.team10.instagram

import com.fasterxml.jackson.databind.ObjectMapper
import com.team10.instagram.domain.comment.dto.CommentCreateRequest
import com.team10.instagram.domain.comment.dto.CommentUpdateRequest
import com.team10.instagram.domain.comment.model.CommentLike
import com.team10.instagram.domain.comment.repository.CommentLikeRepository
import com.team10.instagram.domain.comment.repository.CommentRepository
import com.team10.instagram.domain.user.model.User
import com.team10.instagram.helper.DataGenerator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.Executors

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommentIntegrationTest
    @Autowired
    constructor(
        private val mvc: MockMvc,
        private val dataGenerator: DataGenerator,
        private val commentRepository: CommentRepository,
        @Autowired private val commentLikeRepository: CommentLikeRepository,
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
        fun `댓글을 작성할 수 있다`() {
            // given
            val user = myUser
            val token = myToken
            val post = dataGenerator.generatePost(user = user)

            val request = CommentCreateRequest(content = "좋은 사진이네요!")

            // when & then
            mvc
                .perform(
                    post("/api/v1/posts/${post.id}/comments")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content").value("좋은 사진이네요!"))
                .andExpect(jsonPath("$.data.likeCount").value(0))
                .andExpect(jsonPath("$.data.isLiked").value(false))
                .andExpect(jsonPath("$.data.likedUserIds").isEmpty)
        }

        @Test
        fun `빈 내용으로 댓글을 작성하면 실패한다`() {
            // given
            val user = myUser
            val token = myToken
            val post = dataGenerator.generatePost(user = user)
            val request = CommentCreateRequest(content = "")

            // when & then
            mvc
                .perform(
                    post("/api/v1/posts/${post.id}/comments")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest) // 400 Bad Request
        }

        @Test
        fun `게시글의 댓글 목록을 조회할 수 있다`() {
            // given
            val user = myUser
            val token = myToken
            val post = dataGenerator.generatePost(user = user)

            val comment1 = dataGenerator.generateComment(post = post, user = user, content = "댓글1")
            val comment2 = dataGenerator.generateComment(post = post, user = user, content = "댓글2")

            commentLikeRepository.save(CommentLike(commentId = comment1.id!!, userId = user.userId!!))

            // when & then
            mvc
                .perform(
                    get("/api/v1/posts/${post.id}/comments")
                        .header("Authorization", token),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.length()").value(2))
                // Check sort by time (Lastest comment is first)
                .andExpect(jsonPath("$.data[0].content").value("댓글2"))
                .andExpect(jsonPath("$.data[0].likeCount").value(0))
                .andExpect(jsonPath("$.data[0].isLiked").value(false))
                .andExpect(jsonPath("$.data[1].content").value("댓글1"))
                .andExpect(jsonPath("$.data[1].likeCount").value(1))
                .andExpect(jsonPath("$.data[1].isLiked").value(true))
                .andExpect(jsonPath("$.data[1].likedUserIds[0]").value(user.userId))
        }

        @Test
        fun `댓글을 수정할 수 있다`() {
            // given
            val user = myUser
            val token = myToken
            val post = dataGenerator.generatePost(user = user)
            val comment = dataGenerator.generateComment(post = post, user = user, content = "수정 전 댓글")

            val request = CommentUpdateRequest(content = "수정 후 댓글")

            // when & then
            mvc
                .perform(
                    put("/api/v1/posts/${post.id}/comments/${comment.id}")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content").value("수정 후 댓글"))
        }

        @Test
        fun `빈 내용으로 댓글을 수정하면 실패한다`() {
            // given
            val user = myUser
            val token = myToken
            val post = dataGenerator.generatePost(user = user)
            val comment = dataGenerator.generateComment(post = post, user = user, content = "수정 전 댓글")

            val request = CommentUpdateRequest(content = "")

            // when & then
            mvc
                .perform(
                    put("/api/v1/posts/${post.id}/comments/${comment.id}")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest) // 400 Bad Request
        }

        @Test
        fun `다른 유저의 댓글을 수정하려 하면 실패한다`() {
            // given
            val user = myUser
            val token = myToken
            val otherUser = dataGenerator.generateUser(nickname = "other")
            val post = dataGenerator.generatePost(user = otherUser)
            val comment = dataGenerator.generateComment(post = post, user = otherUser, content = "다른 사람의 댓글")

            val request = CommentUpdateRequest(content = "다른 유저의 댓글에 대한 수정 요청")

            // when & then
            mvc
                .perform(
                    put("/api/v1/posts/${post.id}/comments/${comment.id}")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isForbidden) // 403 Forbidden
        }

        @Test
        fun `댓글을 삭제할 수 있다`() {
            // given
            val user = myUser
            val token = myToken
            val post = dataGenerator.generatePost(user = user)
            val comment = dataGenerator.generateComment(post = post, user = user)

            // when & then
            mvc
                .perform(
                    delete("/api/v1/posts/${post.id}/comments/${comment.id}")
                        .header("Authorization", token),
                ).andExpect(status().isOk)

            assertNull(commentRepository.findByIdOrNull(comment.id!!))
        }

        @Test
        fun `다른 유저의 댓글을 삭제하려 하면 실패한다`() {
            // given
            val user = myUser
            val token = myToken
            val otherUser = dataGenerator.generateUser(nickname = "other")
            val post = dataGenerator.generatePost(user = otherUser)
            val comment = dataGenerator.generateComment(post = post, user = otherUser)

            // when & then
            mvc
                .perform(
                    delete("/api/v1/posts/${post.id}/comments/${comment.id}")
                        .header("Authorization", myToken),
                ).andExpect(status().isForbidden) // 403 Forbidden
        }

        @Test
        fun `댓글 좋아요 및 취소를 할 수 있다`() {
            // given
            val user = myUser
            val token = myToken
            val post = dataGenerator.generatePost(user = user)
            val comment = dataGenerator.generateComment(post = post, user = user)

            // Like
            mvc
                .perform(
                    post("/api/v1/posts/${post.id}/comments/${comment.id}/like")
                        .header("Authorization", token),
                ).andExpect(status().isOk)

            assertTrue(commentLikeRepository.existsByCommentIdAndUserId(comment.id!!, user.userId!!))

            // Unlike
            mvc
                .perform(
                    delete("/api/v1/posts/${post.id}/comments/${comment.id}/like")
                        .header("Authorization", token),
                ).andExpect(status().isOk)

            assertFalse(commentLikeRepository.existsByCommentIdAndUserId(comment.id!!, user.userId!!))
        }
    }

@SpringBootTest
@AutoConfigureMockMvc
class CommentConcurrencyTest
    @Autowired
    constructor(
        private val mvc: MockMvc,
        private val dataGenerator: DataGenerator,
        private val commentLikeRepository: CommentLikeRepository,
    ) {
        @Test
        fun `댓글에 좋아요 등록을 동시에 여러 번 해도 좋아요 수는 1만 올라간다`() {
            // given
            val threadPool = Executors.newFixedThreadPool(4)
            val post = dataGenerator.generatePost(user = dataGenerator.generateUser())
            val comment = dataGenerator.generateComment(post = post, user = dataGenerator.generateUser())
            val user = dataGenerator.generateUser()
            val token = "Bearer ${dataGenerator.generateToken(user)}"

            // when
            val jobs =
                List(4) {
                    threadPool.submit {
                        mvc
                            .perform(
                                post("/api/v1/posts/${post.id}/comments/${comment.id}/like")
                                    .header("Authorization", token)
                                    .contentType(MediaType.APPLICATION_JSON),
                            ).andExpect(status().isOk)
                    }
                }
            jobs.forEach { it.get() }

            // then
            mvc
                .perform(
                    get("/api/v1/posts/${post.id}/comments")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data[0].id").value(comment.id))
                .andExpect(jsonPath("$.data[0].likeCount").value(1))
                .andExpect(jsonPath("$.data[0].isLiked").value(true))
        }

        @Test
        fun `댓글에 좋아요 취소를 동시에 여러 번 해도 좋아요 수는 1만 내려간다`() {
            // given
            val threadPool = Executors.newFixedThreadPool(4)
            val post = dataGenerator.generatePost(user = dataGenerator.generateUser())
            val comment = dataGenerator.generateComment(post = post, user = dataGenerator.generateUser())
            val user = dataGenerator.generateUser()
            val token = "Bearer ${dataGenerator.generateToken(user)}"

            commentLikeRepository.save(CommentLike(commentId = comment.id!!, userId = user.userId!!))

            // when
            val jobs =
                List(4) {
                    threadPool.submit {
                        mvc
                            .perform(
                                delete("/api/v1/posts/${post.id}/comments/${comment.id}/like")
                                    .header("Authorization", token)
                                    .contentType(MediaType.APPLICATION_JSON),
                            ).andExpect(status().isOk)
                    }
                }
            jobs.forEach { it.get() }

            // then
            mvc
                .perform(
                    get("/api/v1/posts/${post.id}/comments")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data[0].likeCount").value(0))
                .andExpect(jsonPath("$.data[0].isLiked").value(false))
        }
    }
