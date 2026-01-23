package com.team10.instagram.global.error

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String,
) {
    // 공통 에러
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "서버 내부 에러가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "400", "입력값이 올바르지 않습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "403", "접근 권한이 없습니다."),

    // 유저 관련 에러 (예시)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "존재하지 않는 회원입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "401", "비밀번호가 일치하지 않습니다."),

    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "409", "이미 가입된 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "409", "이미 가입된 닉네임입니다."),

    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "401", "인증 정보가 유효하지 않습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "401", "인증 정보가 만료되었습니다."),
    REFRESH_TOKEN_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "401", "재발급 토큰이 재사용되었습니다."),

    // 게시글 관련 에러 (예시)
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "게시글을 찾을 수 없습니다."),
    EMPTY_CONTENT(HttpStatus.BAD_REQUEST, "400", "내용이 비어 있습니다."),

    // 댓글 관련 에러
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "댓글을 찾을 수 없습니다."),

    // 팔로우 관련 에러
    SELF_FOLLOW_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "FOLLOW_400_1", "자기 자신은 팔로우할 수 없습니다."),

    OAUTH_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "401", "OAuth 인증에 실패했습니다."),
}
