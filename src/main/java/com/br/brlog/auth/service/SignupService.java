package com.br.brlog.auth.service;

import com.br.brlog.user.dao.UserDAO;
import com.br.brlog.user.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * 이메일 존재 여부 확인
     */
    public boolean isEmailExists(String email) {
        UserDTO user = userDAO.findByUserId(email);
        return user != null;
    }

    /**
     * 사용자 등록
     */
    @Transactional
    public void registerUser(com.br.brlog.auth.dto.SignupRequestDTO request) {
        log.debug("회원가입 처리 시작: {}", request.getUserId());
        
        // 이메일 중복 확인
        if (isEmailExists(request.getUserId())) {
            throw new RuntimeException("이미 등록된 이메일입니다.");
        }
        
        // 이메일 인증 여부 확인
        if (!emailService.isEmailVerified(request.getUserId())) {
            throw new RuntimeException("이메일 인증이 완료되지 않았습니다.");
        }
        
        // 사용자 정보 생성
        UserDTO userDTO = UserDTO.builder()
                .userId(request.getUserId())
                .userNm(request.getUserNm())
                //.userPw(passwordEncoder.encode(request.getUserPw()))
                .userPw(request.getUserPw())
                .userLevel(request.getUserLevel() != null ? request.getUserLevel() : "USER")
                .enabledYn("Y")
                .verifiedYn("Y")
                .build();
        
        // 사용자 저장
        userDAO.saveUser(userDTO);
        
        log.debug("회원가입 처리 완료: {}", request.getUserId());
    }
}