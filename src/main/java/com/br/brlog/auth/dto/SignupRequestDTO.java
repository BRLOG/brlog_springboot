package com.br.brlog.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequestDTO {
    
    @NotBlank(message = "사용자 ID는 필수 입력값입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String userId;
    
    @NotBlank(message = "사용자 이름은 필수 입력값입니다.")
    private String userNm;
    
    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    private String userPw;
    
    private String userLevel;
}