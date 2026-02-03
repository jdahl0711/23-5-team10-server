package com.team10.instagram.global.common

import com.fasterxml.jackson.annotation.JsonProperty

// 모든 응답은 이 껍데기로 감싸서 나갑니다.
data class ApiResponse<T>(
    @JsonProperty("isSuccess")
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    // 실제 데이터 (DTO 등)
    val data: T?,
) {
    companion object {
        // 성공했을 때: ApiResponse.onSuccess(데이터)
        fun <T> onSuccess(data: T): ApiResponse<T> =
            ApiResponse(
                // 성공 여부 (Boolean)
                isSuccess = true,
                // 응답 코드 (String)
                code = "SUCCESS",
                // 메시지 (String)
                message = "요청에 성공하였습니다.",
                // 실제 들어갈 데이터 (Generic)
                data = data,
            )

        // 실패했을 때: ApiResponse.onFailure("400", "잘못된 요청")
        fun onFailure(
            code: String,
            message: String,
        ): ApiResponse<Nothing> =
            ApiResponse(
                isSuccess = false,
                code = code,
                message = message,
                data = null,
            )

        // 유효성 검사 실패 시
        fun <T> onFailure(
            code: String,
            message: String,
            data: T,
        ): ApiResponse<T> =
            ApiResponse(
                isSuccess = false,
                code = code,
                message = message,
                data = data,
            )
    }
}
