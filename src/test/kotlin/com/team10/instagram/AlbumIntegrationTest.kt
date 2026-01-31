package com.team10.instagram

import com.fasterxml.jackson.databind.ObjectMapper
import com.team10.instagram.domain.album.dto.AlbumCreateRequest
import com.team10.instagram.domain.album.repository.AlbumRepository
import com.team10.instagram.domain.post.repository.PostRepository
import com.team10.instagram.domain.user.model.User
import com.team10.instagram.helper.DataGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AlbumIntegrationTest
    @Autowired
    constructor(
        private val mvc: MockMvc,
        private val dataGenerator: DataGenerator,
        private val albumRepository: AlbumRepository,
        private val postRepository: PostRepository,
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
        fun `앨범을 생성할 수 있다`() {
            // given
            val request = AlbumCreateRequest(title = "제주도 여행")

            // when & then
            mvc
                .perform(
                    post("/api/v1/albums")
                        .header("Authorization", myToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data").isNumber)
        }

        @Test
        fun `특정 유저(나)의 앨범 목록을 조회할 수 있다`() {
            // given
            val albumId1 = dataGenerator.generateAlbum(myUser, "맛집 모음")
            val albumId2 = dataGenerator.generateAlbum(myUser, "풍경 사진")

            // 앨범1에 게시글 추가
            val post1 = dataGenerator.generatePost(myUser, images = listOf("http://thumb1.com"))
            val post2 = dataGenerator.generatePost(myUser, images = listOf("http://thumb2.com"))

            albumRepository.updatePostAlbum(post1.id!!, albumId1)
            albumRepository.updatePostAlbum(post2.id!!, albumId1)

            // when & then
            mvc
                .perform(
                    get("/api/v1/albums/users/${myUser.userId}")
                        .header("Authorization", myToken),
                ).andExpect(status().isOk)
                // '앨범 없음'이 항상 포함되므로 총 개수는 3개
                .andExpect(jsonPath("$.data.length()").value(3))
                // 0번째는 무조건 '앨범 없음' (게시글 없으므로 count 0)
                .andExpect(jsonPath("$.data[0].title").value("앨범 없음"))
                .andExpect(jsonPath("$.data[0].postCount").value(0))
                // 그 뒤로 최신순 정렬 (풍경 사진 -> 맛집 모음)
                .andExpect(jsonPath("$.data[1].title").value("풍경 사진"))
                .andExpect(jsonPath("$.data[2].title").value("맛집 모음"))
                .andExpect(jsonPath("$.data[2].postCount").value(2))
        }

        @Test
        fun `타인의 앨범 목록을 조회할 수 있다`() {
            // given
            val otherUser = dataGenerator.generateUser(nickname = "other")
            dataGenerator.generateAlbum(otherUser, "타인의 앨범")
            // 타인의 미지정 게시글 생성 (-1번 앨범 확인용)
            dataGenerator.generatePost(otherUser)

            // when & then
            mvc
                .perform(
                    get("/api/v1/albums/users/${otherUser.userId}")
                        .header("Authorization", myToken),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.length()").value(2)) // 앨범 없음 + 타인의 앨범
                .andExpect(jsonPath("$.data[0].title").value("앨범 없음"))
                .andExpect(jsonPath("$.data[1].title").value("타인의 앨범"))
        }

        @Test
        fun `앨범 상세(게시글 목록)를 조회할 수 있다`() {
            // given
            val albumId = dataGenerator.generateAlbum(myUser, "추억")
            val post = dataGenerator.generatePost(myUser)
            albumRepository.updatePostAlbum(post.id!!, albumId)

            // when & then
            mvc
                .perform(
                    get("/api/v1/albums/$albumId")
                        .header("Authorization", myToken),
                ).andExpect(status().isOk)
                .andExpect(jsonPath("$.data.title").value("추억"))
                .andExpect(jsonPath("$.data.posts.length()").value(1))
                .andExpect(jsonPath("$.data.posts[0].postId").value(post.id))
        }

        @Test
        fun `앨범에 게시글을 추가할 수 있다`() {
            // given
            val albumId = dataGenerator.generateAlbum(myUser)
            val post = dataGenerator.generatePost(myUser)

            // when & then
            mvc
                .perform(
                    post("/api/v1/albums/$albumId/posts/${post.id}")
                        .header("Authorization", myToken),
                ).andExpect(status().isOk)

            // DB 검증
            val savedPost = postRepository.findByIdOrNull(post.id!!)
            assertEquals(albumId, savedPost?.albumId)
        }

        @Test
        fun `타인의 게시글을 내 앨범에 담으려 하면 실패한다`() {
            // given
            val otherUser = dataGenerator.generateUser(nickname = "other")
            val otherPost = dataGenerator.generatePost(otherUser)
            val myAlbumId = dataGenerator.generateAlbum(myUser)

            // when & then
            mvc
                .perform(
                    post("/api/v1/albums/$myAlbumId/posts/${otherPost.id}")
                        .header("Authorization", myToken),
                ).andExpect(status().isForbidden)
        }

        @Test
        fun `타인의 앨범에 내 게시글을 담으려 하면 실패한다`() {
            // given
            val otherUser = dataGenerator.generateUser(nickname = "other")
            val otherAlbumId = dataGenerator.generateAlbum(otherUser)
            val myPost = dataGenerator.generatePost(myUser)

            // when & then
            mvc
                .perform(
                    post("/api/v1/albums/$otherAlbumId/posts/${myPost.id}")
                        .header("Authorization", myToken),
                ).andExpect(status().isForbidden)
        }

        @Test
        fun `앨범 제목을 수정할 수 있다`() {
            // given
            val albumId = dataGenerator.generateAlbum(myUser, "수정 전 제목")
            val request = AlbumCreateRequest(title = "수정 후 제목")

            // when & then
            mvc
                .perform(
                    patch("/api/v1/albums/$albumId")
                        .header("Authorization", myToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andExpect(status().isOk)

            // DB 검증
            val updatedAlbum = albumRepository.findById(albumId)
            assertEquals("수정 후 제목", updatedAlbum?.title)
        }

        @Test
        fun `앨범에서 게시글을 제외할 수 있다`() {
            // given
            val albumId = dataGenerator.generateAlbum(myUser)
            val post = dataGenerator.generatePost(myUser)
            albumRepository.updatePostAlbum(post.id!!, albumId)

            // when & then
            mvc
                .perform(
                    delete("/api/v1/albums/$albumId/posts/${post.id}")
                        .header("Authorization", myToken),
                ).andExpect(status().isOk)

            // DB 검증
            val savedPost = postRepository.findByIdOrNull(post.id!!)
            assertNull(savedPost?.albumId)
        }

        @Test
        fun `앨범 삭제 시 게시글은 유지되고 연결만 끊긴다`() {
            // given
            val albumId = dataGenerator.generateAlbum(myUser)
            val post = dataGenerator.generatePost(myUser)
            albumRepository.updatePostAlbum(post.id!!, albumId)

            // when & then
            mvc
                .perform(
                    delete("/api/v1/albums/$albumId")
                        .header("Authorization", myToken),
                ).andExpect(status().isOk)

            // DB 검증
            assertNull(albumRepository.findById(albumId))
            val savedPost = postRepository.findByIdOrNull(post.id!!)
            assertNotNull(savedPost)
            assertNull(savedPost?.albumId)
        }

        @Test
        fun `이미 존재하는 앨범 이름으로 생성하면 실패한다`() {
            // given
            val duplicateTitle = "제주도 여행"
            dataGenerator.generateAlbum(myUser, duplicateTitle)

            val request = AlbumCreateRequest(title = duplicateTitle)

            // when & then
            mvc
                .perform(
                    post("/api/v1/albums")
                        .header("Authorization", myToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)),
                ).andDo(print())
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.code").value("ALBUM_ALREADY_EXISTS"))
        }

        @Test
        fun `다른 앨범에 있는 게시글을 제외하려고 하면 실패한다`() {
            // given
            val albumAId = dataGenerator.generateAlbum(myUser, "앨범 A")
            val albumBId = dataGenerator.generateAlbum(myUser, "앨범 B")
            val post = dataGenerator.generatePost(myUser)

            albumRepository.updatePostAlbum(post.id!!, albumBId)

            // when & then
            // 내 앨범은 맞지만 잘못된 요청
            mvc
                .perform(
                    delete("/api/v1/albums/$albumAId/posts/${post.id}")
                        .header("Authorization", myToken),
                ).andDo(print())
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.code").value("POST_NOT_IN_ALBUM"))
        }

        @Test
        fun `존재하지 않는 게시글을 앨범에 추가하면 실패한다`() {
            // given
            val albumId = dataGenerator.generateAlbum(myUser)

            val wrongPostId = 99999L // 존재하지 않는 ID

            // when & then
            mvc
                .perform(
                    post("/api/v1/albums/$albumId/posts/$wrongPostId")
                        .header("Authorization", myToken),
                ).andDo(print())
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
        }

        @Test
        fun `앨범에 속하지 않은 게시글이 있으면 '앨범 없음' 앨범으로 조회된다`() {
            // given
            dataGenerator.generateAlbum(myUser, "내 앨범")
            dataGenerator.generatePost(myUser, images = listOf("http://no-album.com")) // 미지정

            // when
            mvc
                .perform(
                    get("/api/v1/albums/users/${myUser.userId}")
                        .header("Authorization", myToken),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.length()").value(2))
                // 2. 맨 첫 번째(0번 인덱스)가 '앨범 없음' 앨범이어야 함
                .andExpect(jsonPath("$.data[0].albumId").value(-1)) // 약속된 ID -1
                .andExpect(jsonPath("$.data[0].title").value("앨범 없음"))
                .andExpect(jsonPath("$.data[1].title").value("내 앨범"))
        }

        @Test
        fun `앨범 미지정 게시글이 없어도 '앨범 없음' 앨범은 항상 보인다`() {
            // given
            val albumId = dataGenerator.generateAlbum(myUser, "내 앨범")
            val post = dataGenerator.generatePost(myUser)
            // 게시글을 앨범에 넣음 -> 미지정 게시글 0개
            albumRepository.updatePostAlbum(post.id!!, albumId)

            // when
            mvc
                .perform(
                    get("/api/v1/albums/users/${myUser.userId}")
                        .header("Authorization", myToken),
                ).andDo(print())
                .andExpect(status().isOk)
                // then
                // 내 앨범 1개 + 빈 '앨범 없음' 1개 = 총 2개
                .andExpect(jsonPath("$.data.length()").value(2))
                // '앨범 없음' 폴더가 0번째에 존재
                .andExpect(jsonPath("$.data[0].albumId").value(-1))
                .andExpect(jsonPath("$.data[0].title").value("앨범 없음"))
                .andExpect(jsonPath("$.data[0].postCount").value(0)) // 개수는 0개
                // 1번째는 내 앨범
                .andExpect(jsonPath("$.data[1].title").value("내 앨범"))
        }

        @Test
        fun `'앨범 없음(-1)' 상세 조회 시, 나의 미지정 게시글 목록이 반환된다`() {
            // given
            val post1 = dataGenerator.generatePost(myUser, images = listOf("http://img1.com"))
            val post2 = dataGenerator.generatePost(myUser, images = listOf("http://img2.com"))

            // when
            // ownerId 파라미터가 없으면 '내 것'으로 동작
            mvc
                .perform(
                    get("/api/v1/albums/-1")
                        .header("Authorization", myToken),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.title").value("앨범 없음"))
                .andExpect(jsonPath("$.data.posts.length()").value(2))
                .andExpect(jsonPath("$.data.posts[0].postId").value(post2.id))
                .andExpect(jsonPath("$.data.posts[0].imageUrl").value("http://img2.com"))
                .andExpect(jsonPath("$.data.posts[1].postId").value(post1.id))
        }

        @Test
        fun `타인의 '앨범 없음(-1)' 상세 조회 시, 해당 유저의 미지정 게시글이 반환된다`() {
            // given
            val otherUser = dataGenerator.generateUser(nickname = "other")
            val otherPost = dataGenerator.generatePost(otherUser, images = listOf("http://other.com"))

            // 내 미지정 게시글
            dataGenerator.generatePost(myUser, images = listOf("http://me.com"))

            // when
            // ?ownerId={otherUserId} 파라미터 전달
            mvc
                .perform(
                    get("/api/v1/albums/-1")
                        .param("ownerId", otherUser.userId.toString())
                        .header("Authorization", myToken),
                ).andDo(print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.title").value("앨범 없음"))
                .andExpect(jsonPath("$.data.posts.length()").value(1))
                // 타인의 게시글만 보여야 함
                .andExpect(jsonPath("$.data.posts[0].imageUrl").value("http://other.com"))
        }

        @Test
        fun `앨범 상세 조회 시 좋아요와 댓글 개수가 포함된다`() {
            // given
            val albumId = dataGenerator.generateAlbum(myUser, "인기 앨범")
            val post = dataGenerator.generatePost(myUser)
            albumRepository.updatePostAlbum(post.id!!, albumId)

            dataGenerator.generateComment(post, myUser, "댓글1")
            dataGenerator.generateComment(post, myUser, "댓글2")

            dataGenerator.generateLike(post, myUser)

            // when
            mvc
                .perform(
                    get("/api/v1/albums/$albumId")
                        .header("Authorization", myToken),
                ).andDo(print())
                .andExpect(status().isOk)
                // then
                .andExpect(jsonPath("$.data.posts[0].postId").value(post.id))
                .andExpect(jsonPath("$.data.posts[0].commentCount").value(2))
                .andExpect(jsonPath("$.data.posts[0].likeCount").value(1))
        }
    }
