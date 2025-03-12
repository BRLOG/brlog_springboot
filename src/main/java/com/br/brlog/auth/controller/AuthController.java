package com.br.brlog.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.brlog.auth.dto.SignupRequestDTO;
import com.br.brlog.auth.service.AuthService;
import com.br.brlog.common.ResponseCode;
import com.br.brlog.common.dto.ResponseDTO;
import com.br.brlog.user.dto.LoginRequestDTO;
import com.br.brlog.user.dto.TokenResponseDTO;
import com.br.brlog.user.dto.UserResponseDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * 로그인 API
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/login")
    public ResponseEntity<ResponseDTO<TokenResponseDTO>> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        try {
            TokenResponseDTO tokenResponse = authService.login(loginRequest);
            return ResponseEntity.ok(ResponseDTO.from(tokenResponse));
        } catch (Exception e) {
            log.error("로그인 실패", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseDTO.of(null, ResponseCode.ERROR_CODE));
        }
    }
    
    /**
     * 회원가입 API
     */
//    @SuppressWarnings("unchecked")
//    @PostMapping("/signup")
//    public ResponseEntity<ResponseDTO<UserResponseDTO>> signup(@Valid @RequestBody SignupRequestDTO signupRequest) {
//        try {
//            UserResponseDTO userResponse = authService.signup(signupRequest);
//            return ResponseEntity.status(HttpStatus.CREATED)
//                    .body(ResponseDTO.from(userResponse));
//        } catch (Exception e) {
//            log.error("회원가입 실패", e);
//            return ResponseEntity.badRequest()
//                    .body(ResponseDTO.of(null, ResponseCode.ERROR_CODE));
//        }
//    }
    
    /**
     * 로그아웃 API
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/logout")
    public ResponseEntity<ResponseDTO<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            authService.logout(authHeader);
            return ResponseEntity.ok(ResponseDTO.from(null));
        } catch (Exception e) {
            log.error("로그아웃 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseDTO.of(null, ResponseCode.ERROR_CODE));
        }
    }
    
    /**
     * 현재 사용자 정보 조회 API
     */
    @SuppressWarnings("unchecked")
    @GetMapping("/me")
    public ResponseEntity<ResponseDTO<UserResponseDTO>> getCurrentUser(Authentication authentication) {
        try {
            UserResponseDTO userResponse = authService.getCurrentUser(authentication);
            return ResponseEntity.ok(ResponseDTO.from(userResponse));
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseDTO.of(null, ResponseCode.ERROR_CODE));
        }
    }
}