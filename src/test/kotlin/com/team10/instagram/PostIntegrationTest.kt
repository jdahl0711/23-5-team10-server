package com.team10.instagram

import com.fasterxml.jackson.databind.ObjectMapper
import com.team10.instagram.domain.post.dto.PostCreateRequest
import com.team10.instagram.domain.post.dto.PostUpdateRequest
import com.team10.instagram.domain.post.model.Bookmark
import com.team10.instagram.domain.post.model.PostLike
import com.team10.instagram.domain.post.repository.BookmarkRepository
import com.team10.instagram.domain.post.repository.PostLikeRepository
import com.team10.instagram.domain.post.repository.PostRepository
import com.team10.instagram.domain.user.model.User
import com.team10.instagram.helper.DataGenerator
import org.junit.jupiter.api.Assertions.assertEquals
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
class PostIntegrationTest
    @Autowired
    constructor(
        private val mvc: MockMvc,
        private val dataGenerator: DataGenerator,
        private val postRepository: PostRepository,
        private val postLikeRepository: PostLikeRepository,
        @Autowired private val bookmarkRepository: BookmarkRepository,
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
        fun `로그인한 유저는 게시글을 생성할 수 있다`() {
            // given
            val user = myUser
            val token = myToken

            val request =
                PostCreateRequest(
                    content = "새로운 게시글입니다.",
                    albumId = null,
                    imageUrls = listOf("https://s3.aws.com/img1.jpg", "https://s3.aws.com/img2.jpg"),
                )

            // when & then
            mvc
                .perform(
                    post("/api/v1/posts")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content").value("새로운 게시글입니다."))
                .andExpect(jsonPath("$.data.images.length()").value(2))
        }

        @Test
        fun `빈 내용으로 게시글을 생성하면 실패한다`() {
            // given
            val user = myUser
            val token = myToken
            val request =
                PostCreateRequest(
                    content = "",
                    albumId = null,
                )

            // when & then
            mvc
                .perform(
                    post("/api/v1/posts")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest) // 400 Bad Request
        }

        @Test
        fun `게시글을 상세 조회할 수 있다`() {
            // given
            val user = myUser
            val token = myToken

            val otherUser = dataGenerator.generateUser()
            val post = dataGenerator.generatePost(user = otherUser, content = "조회용 게시글")

            // when & then
            mvc
                .perform(
                    get("/api/v1/posts/${post.id}")
                        .header("Authorization", token),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.id").value(post.id))
                .andExpect(jsonPath("$.data.content").value("조회용 게시글"))
                .andExpect(jsonPath("$.data.userId").value(otherUser.userId))
        }

        @Test
        fun `존재하지 않는 게시글을 조회하면 실패한다`() {
            // given
            val user = myUser
            val token = myToken
            val notExistPostId = 99999L

            // when & then
            mvc
                .perform(
                    get("/api/v1/posts/$notExistPostId")
                        .header("Authorization", token),
                ).andExpect(status().isNotFound) // 404 Not Found
        }

        @Test
        fun `특정 유저의 게시글 목록을 조회할 수 있다`() {
            // given
            val user = myUser
            val token = myToken
            val otherUser = dataGenerator.generateUser(nickname = "other")

            val post1 = dataGenerator.generatePost(user = otherUser, content = "첫 번째 게시글")
            val post2 = dataGenerator.generatePost(user = otherUser, content = "두 번째 게시글")

            postLikeRepository.save(PostLike(postId = post2.id!!, userId = user.userId!!))

            val stranger = dataGenerator.generateUser(nickname = "stranger")
            dataGenerator.generatePost(user = stranger, content = "남의 글")

            // when
            mvc
                .perform(
                    get("/api/v1/users/${otherUser.userId}/posts")
                        .header("Authorization", myToken)
                        .contentType(MediaType.APPLICATION_JSON),
                )
                // then
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.posts").isArray)
                .andExpect(jsonPath("$.data.posts.length()").value(2))
                // Check sort by time (Lastest comment is first)
                .andExpect(jsonPath("$.data.posts[0].id").value(post2.id))
                .andExpect(jsonPath("$.data.posts[1].id").value(post1.id))
                .andExpect(jsonPath("$.data.posts[0].content").value("두 번째 게시글"))
                .andExpect(jsonPath("$.data.posts[0].isLiked").value(true))
                .andExpect(jsonPath("$.data.posts[1].isLiked").value(false))
        }

        @Test
        fun `게시글이 없는 유저를 조회하면 빈 리스트가 반환된다`() {
            // given
            val user = myUser
            val token = myToken
            val otherUser = dataGenerator.generateUser(nickname = "other")

            // when
            mvc
                .perform(
                    get("/api/v1/users/${otherUser.userId}/posts")
                        .header("Authorization", myToken),
                )
                // then
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.posts").isArray)
                .andExpect(jsonPath("$.data.posts").isEmpty())
        }

        @Test
        fun `존재하지 않는 유저의 게시글을 조회하면 실패한다`() {
            // given
            val invalidUserId = 99999L

            // when
            mvc
                .perform(
                    get("/api/v1/users/$invalidUserId/posts")
                        .header("Authorization", myToken),
                )
                // then
                .andExpect(status().isNotFound)
        }

        @Test
        fun `작성자는 본인의 게시글을 수정할 수 있다`() {
            // given
            val user = myUser
            val token = myToken

            val post = dataGenerator.generatePost(user = user, content = "수정 전 내용")

            val request =
                PostUpdateRequest(
                    content = "수정 후 내용",
                    albumId = null,
                )

            // when & then
            mvc
                .perform(
                    put("/api/v1/posts/${post.id}")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content").value("수정 후 내용"))

            // DB 확인
            val updatedPost = postRepository.findByIdOrNull(post.id!!)
            assertEquals("수정 후 내용", updatedPost!!.content)
        }

        @Test
        fun `빈 내용으로 게시글을 수정하면 실패한다`() {
            // given
            val user = myUser
            val token = myToken
            val post = dataGenerator.generatePost(user = user, content = "원본 글")

            val request =
                PostUpdateRequest(
                    content = "",
                    albumId = null,
                )

            // when & then
            mvc
                .perform(
                    put("/api/v1/posts/${post.id}")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isBadRequest) // 400 Bad Request
        }

        @Test
        fun `다른 유저의 게시글을 수정하려 하면 실패한다`() {
            // given
            val user = myUser
            val token = myToken
            val otherUser = dataGenerator.generateUser(nickname = "other")
            val post = dataGenerator.generatePost(user = otherUser, content = "원본 글")

            val request = PostUpdateRequest(content = "해킹 시도", albumId = null)

            // when & then
            mvc
                .perform(
                    put("/api/v1/posts/${post.id}")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isForbidden) // 403 Forbidden
        }

        @Test
        fun `작성자는 본인의 게시글을 삭제할 수 있다`() {
            // given
            val user = myUser
            val token = myToken
            val post = dataGenerator.generatePost(user = user)

            // when & then
            mvc
                .perform(
                    delete("/api/v1/posts/${post.id}")
                        .header("Authorization", token),
                ).andExpect(status().isOk)

            // check DB status
            assertNull(postRepository.findByIdOrNull(post.id!!))
        }

        @Test
        fun `다른 유저의 게시글을 삭제하려 하면 실패한다`() {
            // given
            val user = myUser
            val token = myToken
            val otherUser = dataGenerator.generateUser(nickname = "other")
            val post = dataGenerator.generatePost(user = otherUser)

            // when & then
            mvc
                .perform(
                    delete("/api/v1/posts/${post.id}")
                        .header("Authorization", token),
                ).andExpect(status().isForbidden) // 403 Forbidden
        }

        @Test
        fun `게시글 좋아요 및 취소를 할 수 있다`() {
            // given
            val user = myUser
            val token = myToken
            val otherUser = dataGenerator.generateUser()
            val post = dataGenerator.generatePost(user = otherUser)

            // Like
            mvc
                .perform(
                    post("/api/v1/posts/${post.id}/like")
                        .header("Authorization", token),
                ).andExpect(status().isOk)

            assertTrue(postLikeRepository.countByPostId(post.id!!) == 1L)

            // Unlike
            mvc
                .perform(
                    delete("/api/v1/posts/${post.id}/like")
                        .header("Authorization", token),
                ).andExpect(status().isOk)

            assertTrue(postLikeRepository.countByPostId(post.id!!) == 0L)
        }

        @Test
        fun `게시글 북마크 및 취소를 할 수 있다`() {
            // given
            val user = myUser
            val token = myToken
            val otherUser = dataGenerator.generateUser()
            val post = dataGenerator.generatePost(user = otherUser)

            // Bookmark
            mvc
                .perform(
                    post("/api/v1/posts/${post.id}/bookmark")
                        .header("Authorization", token),
                ).andExpect(status().isOk)

            assertTrue(bookmarkRepository.existsByPostIdAndUserId(post.id!!, user.userId!!))

            // Unbookmark
            mvc
                .perform(
                    delete("/api/v1/posts/${post.id}/bookmark")
                        .header("Authorization", token),
                ).andExpect(status().isOk)

            val exists = bookmarkRepository.existsByPostIdAndUserId(post.id!!, user.userId!!)
            assertTrue(!exists)
        }
    }

@SpringBootTest
@AutoConfigureMockMvc
class PostConcurrencyTest
    @Autowired
    constructor(
        private val mvc: MockMvc,
        private val dataGenerator: DataGenerator,
        private val postLikeRepository: PostLikeRepository,
        @Autowired private val bookmarkRepository: BookmarkRepository,
    ) {
        @Test
        fun `게시글에 좋아요 등록을 동시에 여러 번 해도 좋아요 수는 1만 올라간다`() {
            // given
            val threadPool = Executors.newFixedThreadPool(4)
            val post = dataGenerator.generatePost(user = dataGenerator.generateUser())
            val user = dataGenerator.generateUser()
            val token = "Bearer ${dataGenerator.generateToken(user)}"

            // when
            val jobs =
                List(4) {
                    threadPool.submit {
                        mvc
                            .perform(
                                post("/api/v1/posts/${post.id}/like")
                                    .header("Authorization", token)
                                    .contentType(MediaType.APPLICATION_JSON),
                            ).andExpect(status().isOk)
                    }
                }
            jobs.forEach { it.get() }

            // then
            mvc
                .perform(
                    get("/api/v1/posts/${post.id}")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.likeCount").value(1))
        }

        @Test
        fun `게시글에 좋아요 취소를 동시에 여러 번 해도 좋아요 수는 1만 내려간다`() {
            // given
            val threadPool = Executors.newFixedThreadPool(4)
            val post = dataGenerator.generatePost(user = dataGenerator.generateUser())
            val user1 = dataGenerator.generateUser()
            val token1 = "Bearer ${dataGenerator.generateToken(user1)}"
            val user2 = dataGenerator.generateUser()
            val token2 = "Bearer ${dataGenerator.generateToken(user2)}"

            postLikeRepository.save(PostLike(postId = post.id!!, userId = user1.userId!!))
            postLikeRepository.save(PostLike(postId = post.id, userId = user2.userId!!))

            // when
            val jobs =
                List(4) {
                    threadPool.submit {
                        mvc.perform(
                            delete("/api/v1/posts/${post.id}/like")
                                .header("Authorization", token1)
                                .contentType(MediaType.APPLICATION_JSON),
                        )
                    }
                }
            jobs.forEach { it.get() }

            // then
            mvc
                .perform(
                    get("/api/v1/posts/${post.id}")
                        .header("Authorization", token1)
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.likeCount").value(1))
        }

        @Test
        fun `게시글 북마크를 동시에 여러 번 해도 한 번만 적용된다`() {
            // given
            val threadPool = Executors.newFixedThreadPool(4)
            val post = dataGenerator.generatePost(user = dataGenerator.generateUser())
            val user = dataGenerator.generateUser()
            val token = "Bearer ${dataGenerator.generateToken(user)}"

            // when
            val jobs =
                List(4) {
                    threadPool.submit {
                        mvc
                            .perform(
                                post("/api/v1/posts/{postId}/bookmark", post.id)
                                    .header("Authorization", token)
                                    .contentType(MediaType.APPLICATION_JSON),
                            ).andExpect(status().isOk)
                    }
                }

            jobs.forEach { it.get() }

            // then
            mvc
                .perform(
                    get("/api/v1/posts/{postId}", post.id)
                        .header("Authorization", token),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.isBookmarked").value(true))

            val count = bookmarkRepository.count()
            assertTrue(bookmarkRepository.existsByPostIdAndUserId(post.id!!, user.userId!!))
        }

        @Test
        fun `게시글 북마크 취소를 동시에 여러 번 해도 취소는 한 번만 적용된다`() {
            // given
            val threadPool = Executors.newFixedThreadPool(4)
            val post = dataGenerator.generatePost(user = dataGenerator.generateUser())
            val user = dataGenerator.generateUser()
            val token = "Bearer ${dataGenerator.generateToken(user)}"

            bookmarkRepository.save(Bookmark(postId = post.id!!, userId = user.userId!!))

            // when
            val jobs =
                List(4) {
                    threadPool.submit {
                        mvc
                            .perform(
                                delete("/api/v1/posts/{postId}/bookmark", post.id)
                                    .header("Authorization", token)
                                    .contentType(MediaType.APPLICATION_JSON),
                            ).andExpect(status().isOk)
                    }
                }
            jobs.forEach { it.get() }

            // then
            mvc
                .perform(
                    get("/api/v1/posts/{postId}", post.id)
                        .header("Authorization", token),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.isBookmarked").value(false))

            assertFalse(bookmarkRepository.existsByPostIdAndUserId(post.id, user.userId))
        }
    }
