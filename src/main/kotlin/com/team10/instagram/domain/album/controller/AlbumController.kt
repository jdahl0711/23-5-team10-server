package com.team10.instagram.domain.album.controller

import com.team10.instagram.domain.album.dto.AlbumCreateRequest
import com.team10.instagram.domain.album.dto.AlbumDetailResponse
import com.team10.instagram.domain.album.dto.AlbumResponse
import com.team10.instagram.domain.album.service.AlbumService
import com.team10.instagram.domain.user.LoggedInUser
import com.team10.instagram.global.common.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/albums")
@Tag(name = "Album API", description = "앨범 관련 API")
class AlbumController(
    private val albumService: AlbumService,
) {
    // 1. 앨범 생성
    @PostMapping
    @Operation(summary = "앨범 생성", description = "앨범을 생성합니다.")
    fun createAlbum(
        @LoggedInUser loggedInUser: Long,
        @RequestBody request: AlbumCreateRequest,
    ): ApiResponse<Long> {
        val albumId = albumService.createAlbum(loggedInUser, request)
        return ApiResponse.onSuccess(albumId)
    }

    // 2. 특정 유저 앨범 목록 조회
    @GetMapping("/users/{userId}")
    @Operation(summary = "유저 앨범 목록 조회", description = "특정 유저(본인 포함)의 앨범 목록을 조회합니다.")
    fun getUserAlbums(
        @PathVariable userId: Long, // 대상 album 주인
    ): ApiResponse<List<AlbumResponse>> {
        val albums = albumService.getAlbumsByUserId(userId)
        return ApiResponse.onSuccess(albums)
    }

    // 3. 앨범 상세 조회
    @GetMapping("/{albumId}")
    @Operation(summary = "앨범 상세 조회", description = "앨범 ID가 -1일 경우, ownerId를 함께 보내면 해당 유저의 미지정 게시글을 조회합니다.")
    fun getAlbumDetail(
        @LoggedInUser loggedInUser: Long,
        @PathVariable albumId: Long,
        @RequestParam(required = false) ownerId: Long?, // 누구 앨범인지 명시 (선택)
    ): ApiResponse<AlbumDetailResponse> {
        // ownerId가 없으면 loggedInUser로 간주
        val targetUserId = ownerId ?: loggedInUser

        val albumDetails = albumService.getAlbumDetail(targetUserId, albumId)
        return ApiResponse.onSuccess(albumDetails)
    }

    // 4. 앨범에 게시글 추가 또는 이동
    @PostMapping("/{albumId}/posts/{postId}")
    @Operation(
        summary = "앨범에 게시글 추가 (이동)",
        description = "게시글을 특정 앨범에 담습니다. 만약 이미 다른 앨범에 있던 글이라면, 새로운 앨범으로 이동됩니다.",
    )
    fun addPostToAlbum(
        @LoggedInUser loggedInUser: Long,
        @PathVariable albumId: Long,
        @PathVariable postId: Long,
    ): ApiResponse<Unit> {
        albumService.addPostToAlbum(loggedInUser, albumId, postId)
        return ApiResponse.onSuccess(Unit)
    }

    // 5. 앨범 수정 (제목 변경 등)
    @PatchMapping("/{albumId}")
    @Operation(summary = "앨범 수정", description = "앨범의 제목을 수정합니다.")
    fun updateAlbum(
        @LoggedInUser loggedInUser: Long,
        @PathVariable albumId: Long,
        @RequestBody request: AlbumCreateRequest,
    ): ApiResponse<Unit> {
        albumService.updateAlbum(loggedInUser, albumId, request)
        return ApiResponse.onSuccess(Unit)
    }

    // 6. 앨범 삭제
    @DeleteMapping("/{albumId}")
    @Operation(summary = "앨범 삭제", description = "해당 앨범을 삭제합니다..")
    fun deleteAlbum(
        @LoggedInUser loggedInUser: Long,
        @PathVariable albumId: Long,
    ): ApiResponse<Unit> {
        albumService.deleteAlbum(loggedInUser, albumId)
        return ApiResponse.onSuccess(Unit)
    }

    // 7. 앨범에서 게시글 제외
    @DeleteMapping("/{albumId}/posts/{postId}")
    @Operation(
        summary = "앨범에서 게시글 제외",
        description = "게시글을 앨범에서 뺍니다. 게시글 자체가 삭제되지는 않고, '앨범 없음' 상태가 됩니다.",
    )
    fun removePostFromAlbum(
        @LoggedInUser loggedInUser: Long,
        @PathVariable albumId: Long,
        @PathVariable postId: Long,
    ): ApiResponse<Unit> {
        albumService.removePostFromAlbum(loggedInUser, albumId, postId)
        return ApiResponse.onSuccess(Unit)
    }
}
