package com.team10.instagram.global.error

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String,
) {
    // Common (공통 에러)
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 에러가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "입력값이 올바르지 않습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "접근 권한이 없습니다."),

    // User (회원 관련)
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 회원입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "INVALID_PASSWORD", "비밀번호가 일치하지 않습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "NICKNAME_ALREADY_EXISTS", "이미 가입된 닉네임입니다."),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "계정을 찾을 수 없습니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_SEND_FAILED", "이메일 발송에 실패하였습니다."),
    INVALID_NICKNAME_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_NICKNAME_FORMAT", "닉네임 형식이 올바르지 않습니다."),

    // Auth (인증/토큰 관련)
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "인증 정보가 유효하지 않습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", "인증 정보가 만료되었습니다."),
    REFRESH_TOKEN_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REUSE_DETECTED", "재발급 토큰이 재사용되었습니다."),

    // Post (게시글 관련)
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "게시글을 찾을 수 없습니다."),
    EMPTY_CONTENT(HttpStatus.BAD_REQUEST, "EMPTY_CONTENT", "내용이 비어 있습니다."),

    // Comment (댓글 관련)
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다."),

    // Follow (팔로우 관련)
    SELF_FOLLOW_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "SELF_FOLLOW_NOT_ALLOWED", "자기 자신은 팔로우할 수 없습니다."),
    ALREADY_FOLLOWING(HttpStatus.CONFLICT, "ALREADY_FOLLOWING", "이미 팔로우 중인 사용자입니다."),
    NOT_FOLLOWING(HttpStatus.BAD_REQUEST, "NOT_FOLLOWING", "팔로우 관계가 존재하지 않습니다."),

    // Album (앨범 관련)
    ALBUM_NOT_FOUND(HttpStatus.NOT_FOUND, "ALBUM_NOT_FOUND", "앨범을 찾을 수 없습니다."),
    ALBUM_ALREADY_EXISTS(HttpStatus.CONFLICT, "ALBUM_ALREADY_EXISTS", "이미 존재하는 앨범 이름입니다."),
    POST_NOT_IN_ALBUM(HttpStatus.BAD_REQUEST, "POST_NOT_IN_ALBUM", "해당 앨범에 포함된 게시글이 아닙니다."),

    // Story (스토리 관련)
    STORY_NOT_FOUND(HttpStatus.NOT_FOUND, "STORY_NOT_FOUND", "스토리를 찾을 수 없거나 이미 만료되었습니다."),
    STORY_NOT_OWNER(HttpStatus.FORBIDDEN, "STORY_NOT_OWNER", "스토리 삭제 권한이 없습니다."),
}
