package com.team10.instagram.domain.album.service

import com.team10.instagram.domain.album.dto.AlbumCreateRequest
import com.team10.instagram.domain.album.dto.AlbumDetailResponse
import com.team10.instagram.domain.album.dto.AlbumResponse
import com.team10.instagram.domain.album.repository.AlbumRepository
import com.team10.instagram.domain.post.repository.PostRepository
import com.team10.instagram.global.error.CustomException
import com.team10.instagram.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AlbumService(
    private val albumRepository: AlbumRepository,
    private val postRepository: PostRepository,
) {
    // 1. 앨범 생성
    @Transactional
    fun createAlbum(
        userId: Long,
        request: AlbumCreateRequest,
    ): Long {
        checkAlbumNameDuplicate(userId, request.title)

        return albumRepository.save(userId, request.title)
    }

    // 2. 특정 유저의 앨범 목록 조회
    @Transactional(readOnly = true)
    fun getAlbumsByUserId(targetUserId: Long): List<AlbumResponse> = albumRepository.findAllByUserId(targetUserId)

    // 3. 앨범 상세 조회
    @Transactional(readOnly = true)
    fun getAlbumDetail(
        targetUserId: Long, // 앨범 주인 ID
        albumId: Long,
    ): AlbumDetailResponse {
        // 1. '앨범 없음(-1)' 요청인 경우
        if (albumId == -1L) {
            // targetUserId의 글을 가져옴
            val posts = albumRepository.findUnassignedPosts(targetUserId)

            return AlbumDetailResponse(
                albumId = -1L,
                title = "앨범 없음",
                posts = posts,
            )
        }

        // 2. 일반 앨범 요청
        val album =
            albumRepository.findById(albumId)
                ?: throw CustomException(ErrorCode.ALBUM_NOT_FOUND)

        val posts = albumRepository.findPostsByAlbumId(albumId)

        return AlbumDetailResponse(
            albumId = album.albumId,
            title = album.title,
            posts = posts,
        )
    }

    // 4. 앨범에 게시글 추가 (이동)
    @Transactional
    fun addPostToAlbum(
        userId: Long,
        albumId: Long,
        postId: Long,
    ) {
        validateAlbumOwner(userId, albumId)

        // 게시글 존재 여부 및 소유권 확인
        val post =
            postRepository.findById(postId).orElseThrow {
                CustomException(ErrorCode.POST_NOT_FOUND)
            }

        if (post.userId != userId) {
            throw CustomException(ErrorCode.ACCESS_DENIED)
        }

        // 이미 이 앨범에 들어있는 경우
        if (post.albumId == albumId) return

        albumRepository.updatePostAlbum(postId, albumId)
    }

    // 5. 앨범 수정
    @Transactional
    fun updateAlbum(
        userId: Long,
        albumId: Long,
        request: AlbumCreateRequest,
    ) {
        validateAlbumOwner(userId, albumId)
        checkAlbumNameDuplicate(userId, request.title)

        albumRepository.updateTitle(albumId, request.title)
    }

    // 6. 앨범 삭제
    @Transactional
    fun deleteAlbum(
        userId: Long,
        albumId: Long,
    ) {
        validateAlbumOwner(userId, albumId)

        // 1. 게시글들의 album_id를 먼저 NULL로 변경 (참조 무결성 유지)
        albumRepository.detachPostsFromAlbum(albumId)
        // 2. 앨범 삭제
        albumRepository.delete(albumId)
    }

    // 7. 앨범에서 게시글 제외
    @Transactional
    fun removePostFromAlbum(
        userId: Long,
        albumId: Long,
        postId: Long,
    ) {
        validateAlbumOwner(userId, albumId)

        // 게시글을 조회해서 검증
        val post =
            postRepository.findById(postId).orElseThrow {
                CustomException(ErrorCode.POST_NOT_FOUND)
            }

        // 내 글인지 확인
        if (post.userId != userId) {
            throw CustomException(ErrorCode.ACCESS_DENIED)
        }

        // 빼려는 게시글이 이 앨범에 들어있는지 확인
        if (post.albumId != albumId) {
            throw CustomException(ErrorCode.POST_NOT_IN_ALBUM)
        }

        // 해당 게시글의 album_id를 NULL로 변경
        albumRepository.updatePostAlbum(postId, null)
    }

    // 권한 검증
    private fun validateAlbumOwner(
        userId: Long,
        albumId: Long,
    ) {
        val ownerId =
            albumRepository.findOwnerId(albumId)
                ?: throw CustomException(ErrorCode.ALBUM_NOT_FOUND)
        if (ownerId != userId) {
            throw CustomException(ErrorCode.ACCESS_DENIED)
        }
    }

    private fun checkAlbumNameDuplicate(
        userId: Long,
        title: String,
    ) {
        if (albumRepository.existsByUserIdAndTitle(userId, title)) {
            throw CustomException(ErrorCode.ALBUM_ALREADY_EXISTS)
        }
    }
}
