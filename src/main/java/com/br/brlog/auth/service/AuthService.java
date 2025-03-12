package com.br.brlog.auth.service;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.br.brlog.auth.dto.SignupRequestDTO;
import com.br.brlog.user.dto.LoginRequestDTO;
import com.br.brlog.user.dto.TokenResponseDTO;
import com.br.brlog.user.dto.UserResponseDTO;
import com.br.brlog.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    
    /**
     * 로그인 처리
     * @throws Exception 
     */
    public TokenResponseDTO login(LoginRequestDTO loginRequest) throws Exception {
        return userService.login(loginRequest);
    }
    
    /**
     * 회원가입 처리 
     * @throws Exception 
     */
    public UserResponseDTO signup(SignupRequestDTO signupRequest) throws Exception {
        return userService.signup(signupRequest);
    }
    
    /**
     * 로그아웃 처리
     */
    public void logout(String authHeader) {
        userService.logout(authHeader);
    }
    
    /**
     * 현재 인증된 사용자 정보 조회
     * @throws Exception 
     */
    public UserResponseDTO getCurrentUser(Authentication authentication) throws Exception {
        if (authentication == null) {
            return null;
        }
        
        String email = authentication.getName();
        return userService.getCurrentUser(email);
    }
}