package com.team10.instagram.domain.story.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

data class StoryCreateRequest(
    @Schema(description = "업로드할 이미지 URL", example = "https://s3.aws.com/story/1.jpg")
    @field:NotBlank(message = "이미지 URL은 비어있을 수 없습니다.")
    val imageUrl: String,
)

// 1. 스토리 피드(상단 바)에 띄울 "사람" 정보
data class StoryFeedResponse(
    val userId: Long,
    val nickname: String,
    val profileImageUrl: String?,
    @Schema(description = "아직 안 본 스토리가 있는지 여부 (테두리 색상용)", example = "true")
    val hasUnseenStory: Boolean = true,
    @Schema(description = "해당 유저의 스토리 목록")
    val stories: List<StoryDetailResponse> = emptyList(),
)

// 2. 친구 얼굴 눌렀을 때 보여줄 "스토리 상세" 정보
data class StoryDetailResponse(
    val id: Long,
    val userId: Long,
    val imageUrl: String,
    val createdAt: LocalDateTime,
    @Schema(description = "몇 명이 봤는지", example = "15")
    val viewCount: Int?,
)
