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
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다. 다시 인증해주세요."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않거나 변조된 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "유효하지 않은 토큰입니다."),
    TOKEN_MISMATCH(HttpStatus.BAD_REQUEST, "토큰 정보가 일치하지 않습니다."),
    SIGNUP_EMAIL_MISMATCH(HttpStatus.BAD_REQUEST, "회원가입 인증 이메일과 요청 이메일이 일치하지 않습니다."),
    INVALID_USER(HttpStatus.BAD_REQUEST, "유효하지 않은 사용자입니다."),
    USER_WITHDRAWN(HttpStatus.BAD_REQUEST, "탈퇴한 사용자입니다."),

    // User
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "사용자가 존재하지 않습니다."),
    DUPLICATE_NICKNAME(HttpStatus.BAD_REQUEST, "이미 사용 중인 닉네임입니다."),
    DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "사용 중인 이메일입니다."),
    DUPLICATE_VERIFIED_EMAIL(HttpStatus.BAD_REQUEST, "인증에 이용된 이메일입니다."),
    EMAIL_USED_BY_OTHER_ACCOUNT(HttpStatus.BAD_REQUEST, "이미 다른 계정에서 사용 중인 이메일입니다."),
    USER_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "인증되지 않은 사용자입니다."),
    USER_ID_DUPLICATE(HttpStatus.BAD_REQUEST, "중복되는 유저 ID입니다."),
    NICKNAME_CHANGE_COOLDOWN(HttpStatus.BAD_REQUEST, "닉네임은 90일마다 변경할 수 있습니다."),

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
    STACK_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "스택은 최대 7개까지 선택할 수 있습니다."),

    // Univ
    UNIVERSITY_NOT_MATCHED(HttpStatus.BAD_REQUEST, "대학 id와 도메인이 일치하지 않습니다."),

    // Project
    PROJECT_NOT_FOUND(HttpStatus.BAD_REQUEST, "프로젝트를 찾을 수 없습니다."),
    PROJECT_DELETED(HttpStatus.BAD_REQUEST, "삭제된 프로젝트입니다."),
    PROJECT_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "이미 삭제된 프로젝트입니다."),
    AUTHOR_MISMATCHED(HttpStatus.BAD_REQUEST, "작성자가 일치하지 않습니다."),
    PROJECT_NOT_MATCHED(HttpStatus.BAD_REQUEST, "프로젝트가 일치하지 않습니다."),
    USER_NOT_IN_PROJECT(HttpStatus.BAD_REQUEST, "프로젝트에 참여하지 않은 사용자입니다."),
    PROJECT_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "프로젝트가 완료되지 않았습니다."),
    PROJECT_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "프로젝트가 이미 완료되었습니다."),
    PROJECT_MEMBER_NOT_FOUND(HttpStatus.BAD_REQUEST, "프로젝트 멤버를 찾을 수 없습니다."),
    PROJECT_HAS_APPLICATIONS(HttpStatus.BAD_REQUEST, "지원 이력이 있는 프로젝트는 삭제할 수 없습니다."),
    PROJECT_EDIT_CLOSED(HttpStatus.BAD_REQUEST, "모집이 마감된 프로젝트는 수정할 수 없습니다."),
    INVALID_PROJECT_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "현재 단계에서는 요청한 프로젝트 상태로 변경할 수 없습니다."),
    PENDING_APPLICATIONS_EXIST(HttpStatus.BAD_REQUEST, "대기 중인 지원자를 모두 승인하거나 거절한 후 프로젝트를 시작해주세요."),
    PROJECT_CHAT_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "프로젝트가 시작된 후 대화방을 이용할 수 있습니다."),
    PROJECT_CHAT_READ_ONLY(HttpStatus.BAD_REQUEST, "완료된 프로젝트의 대화방은 읽기만 가능합니다."),
    INVALID_CHAT_MESSAGE(HttpStatus.BAD_REQUEST, "메시지는 1자 이상 1000자 이하로 입력해주세요."),

    // Recruitment
    RECRUITMENT_DUPLICATE(HttpStatus.BAD_REQUEST, "중복된 모집입니다."),
    RECRUITMENT_NOT_FOUND(HttpStatus.BAD_REQUEST, "모집 정보를 찾을 수 없습니다."),
    RECRUITMENT_CLOSED(HttpStatus.BAD_REQUEST, "모집이 마감된 모집입니다."),
    RECRUITMENT_EMPTY(HttpStatus.BAD_REQUEST, "모집 정보가 존재하지 않습니다."),
    INVALID_RECRUITMENT_ROLE(HttpStatus.BAD_REQUEST, "모집 직군이 카테고리와 일치하지 않습니다."),
    AUTHOR_NOT_APPLICABLE(HttpStatus.BAD_REQUEST, "작성자가 지원할 수 없습니다."),
    ALREADY_APPLIED(HttpStatus.BAD_REQUEST, "이미 지원한 프로젝트입니다."),
    APPLICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "지원서를 찾을 수 없습니다."),

    //Role
    CUSTOM_JOB_ROLE_REQUIRED(HttpStatus.BAD_REQUEST, "직군이 비어있을 수 없습니다."),
    ROLE_DUPLICATE(HttpStatus.BAD_REQUEST, "중복된 직군입니다."),
    INVALID_JOB_ROLE(HttpStatus.BAD_REQUEST, "직군 요청이 올바르지 않습니다."),
    JOB_ROLE_NOT_FOUND(HttpStatus.BAD_REQUEST, "직군을 찾을 수 없습니다."),
    JOB_CATEGORY_NOT_FOUND(HttpStatus.BAD_REQUEST, "직군 카테고리를 찾을 수 없습니다."),

    // Community
    POST_NOT_FOUND(HttpStatus.BAD_REQUEST, "게시글을 찾을 수 없습니다."),
    POST_DELETED(HttpStatus.BAD_REQUEST, "삭제된 게시글입니다."),
    NOT_POST_AUTHOR(HttpStatus.BAD_REQUEST, "게시글 작성자가 일치하지 않습니다."),
    COMMENT_NOT_FOUND(HttpStatus.BAD_REQUEST, "댓글을 찾을 수 없습니다."),
    COMMENT_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "대댓글은 한 단계까지만 작성할 수 있습니다."),
    NOT_COMMENT_AUTHOR(HttpStatus.BAD_REQUEST, "댓글 작성자가 일치하지 않습니다."),

    // Reviews
    CANNOT_REVIEW_SELF(HttpStatus.BAD_REQUEST, "자기 자신에게 리뷰를 작성할 수 없습니다."),
    REVIEW_TARGET_INVALID(HttpStatus.BAD_REQUEST, "평가 대상이 유효하지 않습니다."),
    ALREADY_REVIEWED(HttpStatus.BAD_REQUEST, "이미 평가를 작성했습니다."),
    REVIEW_NOT_FOUND(HttpStatus.BAD_REQUEST, "리뷰를 찾을 수 없습니다."),

    //Applications
    APPLICATION_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "이미 처리된 지원입니다."),
    APPLICATION_DECISION_CLOSED(HttpStatus.BAD_REQUEST, "진행 중이거나 완료된 프로젝트의 지원자는 처리할 수 없습니다."),

    // Questions
    QUESTION_NOT_FOUND(HttpStatus.BAD_REQUEST, "프로젝트에 존재하지 않는 질문입니다."),
    QUESTION_ANSWER_REQUIRED(HttpStatus.BAD_REQUEST, "필수 질문에 답변해야 합니다."),
    QUESTION_ANSWER_DUPLICATE(HttpStatus.BAD_REQUEST, "같은 질문에 중복으로 답변할 수 없습니다."),
    INVALID_QUESTION_ANSWER(HttpStatus.BAD_REQUEST, "질문의 선택지에 맞지 않는 답변입니다.");

    private final HttpStatus status;
    private final String message;

    public String getCode() {
        return name();
    }
}
