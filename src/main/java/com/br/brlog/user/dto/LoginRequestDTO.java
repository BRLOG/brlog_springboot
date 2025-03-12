package com.br.brlog.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 로그인 요청 DTO
 */
@Data
public class LoginRequestDTO {
    @NotBlank(message = "이메일은 필수 입력 항목입니다")
    @Email(message = "유효한 이메일 주소를 입력해주세요")
    private String userId;

    @NotBlank(message = "비밀번호는 필수 입력 항목입니다")
    private String userPw;
    
    private boolean rememberMe;
}