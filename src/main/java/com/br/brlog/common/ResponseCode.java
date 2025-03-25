package com.br.brlog.common;

import lombok.Getter;

@Getter
public enum ResponseCode {
    SUCCESS_CODE(true, "S000", "성공"),
    AUTH_FAILURE(false, "A001", "인증 실패"),
    TOKEN_INVALID(false, "A002", "토큰이 유효하지 않습니다"),
    UNAUTHORIZED(false, "A003", "권한이 없습니다"),
    
    // 공통 에러
    ERROR_INVALID_PARAMETER(false, "E001", "잘못된 요청 파라미터"),
    ERROR_SERVER(false, "E999", "서버 오류"),
    
    // 일반 오류 코드
    ERROR_CODE(false, "E000", "오류가 발생했습니다"),
    
    // 회원가입 관련 코드
    EMAIL_EXISTS(false, "S001", "이미 등록된 이메일입니다"),
    EMAIL_SEND_FAILURE(false, "S002", "이메일 발송에 실패했습니다"),
    VERIFICATION_FAILED(false, "S003", "인증번호가 일치하지 않습니다"),
    VERIFICATION_ERROR(false, "S004", "인증 과정에서 오류가 발생했습니다"),
    VERIFICATION_EXPIRED(false, "S005", "인증번호가 만료되었습니다"),
    EMAIL_NOT_VERIFIED(false, "S006", "이메일 인증이 완료되지 않았습니다"),
    SIGNUP_FAILURE(false, "S007", "회원가입 처리 중 오류가 발생했습니다"),
	
	// 사용자 관련 에러
    ERROR_USER_NOT_FOUND(false, "U001", "사용자를 찾을 수 없습니다"),
    ERROR_DUPLICATE_USER(false, "U002", "이미 존재하는 사용자입니다"),
    ERROR_INVALID_PASSWORD(false, "U003", "비밀번호가 일치하지 않습니다"),
	
	// 이미지 관련
	ERROR_IMAGE_EXPLAIN(false, "I001", "이미지 설명을 입력해주세요."),
	ERROR_IMAGE_GENERATE(false, "I002", "이미지 생성 중 오류가 발생했습니다.");

    private final boolean result;
    private final String code;
    private final String message;

    ResponseCode(boolean result, String code, String message) {
        this.result = result;
        this.code = code;
        this.message = message;
    }
}