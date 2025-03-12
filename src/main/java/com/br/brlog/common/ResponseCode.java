package com.br.brlog.common;

import lombok.Getter;

@Getter
public enum ResponseCode {
    // 기존 코드
    SUCCESS_CODE(true, "S000", "성공"),
    AUTH_FAILURE(false, "A001", "인증 실패"),
    TOKEN_INVALID(false, "A002", "토큰이 유효하지 않습니다"),
    UNAUTHORIZED(false, "A003", "권한이 없습니다"),
    
    // 일반 오류 코드
    ERROR_CODE(false, "E000", "오류가 발생했습니다"),
    
    // 회원가입 관련 코드
    EMAIL_EXISTS(false, "S001", "이미 등록된 이메일입니다"),
    EMAIL_SEND_FAILURE(false, "S002", "이메일 발송에 실패했습니다"),
    VERIFICATION_FAILED(false, "S003", "인증번호가 일치하지 않습니다"),
    VERIFICATION_ERROR(false, "S004", "인증 과정에서 오류가 발생했습니다"),
    VERIFICATION_EXPIRED(false, "S005", "인증번호가 만료되었습니다"),
    EMAIL_NOT_VERIFIED(false, "S006", "이메일 인증이 완료되지 않았습니다"),
    SIGNUP_FAILURE(false, "S007", "회원가입 처리 중 오류가 발생했습니다");

    private final boolean result;
    private final String code;
    private final String message;

    ResponseCode(boolean result, String code, String message) {
        this.result = result;
        this.code = code;
        this.message = message;
    }
}