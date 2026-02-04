package com.team10.instagram.domain.album.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AlbumCreateRequest(
    @field:NotBlank(message = "앨범 제목은 필수입니다.")
    @field:Size(max = 50, message = "앨범 제목은 50자를 초과할 수 없습니다.")
    val title: String,
)

data class AlbumResponse(
    val albumId: Long,
    val title: String,
    val thumbnailImageUrl: String?, // 앨범 대표 이미지 (없으면 null)
    val postCount: Int,
)

data class AlbumDetailResponse(
    val albumId: Long,
    val title: String,
    val posts: List<AlbumPostDto>,
)

data class AlbumPostDto(
    val postId: Long,
    val imageUrl: String,
    val likeCount: Int,
    val commentCount: Int,
)
