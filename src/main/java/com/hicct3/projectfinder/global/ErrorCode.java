package com.hicct3.projectfinder.global;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Common
    COMMON_BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "요청 본문 형식이 올바르지 않습니다."),
    MISSING_HEADER(HttpStatus.BAD_REQUEST, "필수 헤더가 누락되었습니다."),

    // Auth
    AUTHENTICATION_FAILED(HttpStatus.BAD_REQUEST, "이메일 또는 비밀번호가 일치하지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 토큰입니다."),
    TOKEN_MISMATCH(HttpStatus.BAD_REQUEST, "토큰 정보가 일치하지 않습니다."),
    SIGNUP_EMAIL_MISMATCH(HttpStatus.BAD_REQUEST, "회원가입 인증 이메일과 요청 이메일이 일치하지 않습니다."),
    INVALID_USER(HttpStatus.BAD_REQUEST, "유효하지 않은 사용자입니다."),

    // User
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "사용자가 존재하지 않습니다."),
    DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST, "이미 사용 중인 닉네임입니다."),
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "사용 중인 이메일입니다."),
    DUPLICATE_VERIFIED_EMAIL(HttpStatus.BAD_REQUEST, "인증에 이용된 이메일입니다."),
    EMAIL_USED_BY_OTHER_ACCOUNT(HttpStatus.BAD_REQUEST, "이미 다른 계정에서 사용 중인 이메일입니다."),
    USER_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "인증되지 않은 사용자입니다."),

    // Email
    EMAIL_SEND_FAILED(HttpStatus.BAD_REQUEST, "이메일 전송에 실패했습니다."),
    EMAIL_VERIFICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "이메일 인증 코드가 존재하지 않습니다."),
    EMAIL_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "이메일 인증 코드가 일치하지 않습니다."),
    EMAIL_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "이메일 인증 코드가 만료되었습니다."),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "올바르지 않은 이메일 형식입니다."),
    NOT_UNIVERSITY_EMAIL(HttpStatus.BAD_REQUEST, "대학 이메일이 아닙니다."),
    UNIVERSITY_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 대학입니다."),

    // Stack
    STACK_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 스택입니다."),
    STACK_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 존재하는 스택입니다."),

    // Univ
    UNIVERSITY_NOT_MATCHED(HttpStatus.BAD_REQUEST, "대학 id와 도메인이 일치하지 않습니다."),

    // Project
    PROJECT_NOT_FOUND(HttpStatus.BAD_REQUEST, "프로젝트를 찾을 수 없습니다."),
    RECRUITMENT_EMPTY(HttpStatus.BAD_REQUEST, "모집 정보가 존재하지 않습니다."),
    PROJECT_DELETED(HttpStatus.BAD_REQUEST, "삭제된 프로젝트입니다."),
    PROJECT_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 프로젝트입니다."),
    AUTHOR_MISMATCHED(HttpStatus.BAD_REQUEST, "작성자가 일치하지 않습니다.");

    private final HttpStatus status;
    private final String message;

    public String getCode() {
        return name();
    }
}