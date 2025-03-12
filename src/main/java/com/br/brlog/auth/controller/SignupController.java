package com.br.brlog.auth.controller;

import com.br.brlog.auth.dto.CheckEmailRequestDTO;
import com.br.brlog.auth.dto.SendVerificationRequestDTO;
import com.br.brlog.auth.dto.SendVerificationResponseDTO;
import com.br.brlog.auth.dto.SignupRequestDTO;
import com.br.brlog.auth.dto.CheckEmailResponseDTO;
import com.br.brlog.auth.dto.VerifyCodeRequestDTO;
import com.br.brlog.auth.dto.VerifyCodeResponseDTO;
import com.br.brlog.auth.service.EmailService;
import com.br.brlog.auth.service.SignupService;
import com.br.brlog.common.ResponseCode;
import com.br.brlog.common.dto.ResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class SignupController {

    private final SignupService signupService;
    private final EmailService emailService;

    /**
     * 이메일 중복 확인
     */
    @SuppressWarnings("unchecked")
	@PostMapping("/check-email")
    public ResponseEntity<ResponseDTO<CheckEmailResponseDTO>> checkEmail(@RequestBody @Valid CheckEmailRequestDTO request) {
        log.debug("이메일 중복 확인 요청: {}", request.getEmail());
        
        boolean exists = signupService.isEmailExists(request.getEmail());
        
        CheckEmailResponseDTO response = new CheckEmailResponseDTO(exists);
        log.debug("이메일 중복 확인 결과: {}", response);
        
        return ResponseEntity.ok(ResponseDTO.from(response));
    }

    /**
     * 이메일 인증번호 발송
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/send-verification")
    public ResponseEntity<ResponseDTO<SendVerificationResponseDTO>> sendVerification(
            @RequestBody @Valid SendVerificationRequestDTO request) {
        log.debug("인증번호 발송 요청: {}", request);
        
        try {
            SendVerificationResponseDTO response = emailService.sendVerificationEmail(
                    request.getEmail(), 
                    request.getName()
            );
            
            log.debug("인증번호 발송 성공: {}", response.getVerificationId());
            
            return ResponseEntity.ok(ResponseDTO.from(response));
        } catch (Exception e) {
            log.error("인증번호 발송 실패: {}", e.getMessage(), e);
            
            ResponseDTO<SendVerificationResponseDTO> errorResponse = 
                ResponseDTO.of(null, ResponseCode.EMAIL_SEND_FAILURE);
            errorResponse.setDetail(e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 인증번호 확인
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/verify-code")
    public ResponseEntity<ResponseDTO<VerifyCodeResponseDTO>> verifyCode(
            @RequestBody @Valid VerifyCodeRequestDTO request) {
        log.debug("인증번호 확인 요청: {}", request);
        
        try {
            boolean verified = emailService.verifyCode(
                    request.getEmail(),
                    request.getVerificationId(),
                    request.getCode()
            );
            
            VerifyCodeResponseDTO response = new VerifyCodeResponseDTO(verified);
            log.debug("인증번호 확인 결과: {}", response);
            
            ResponseCode responseCode = verified ? 
                ResponseCode.SUCCESS_CODE : ResponseCode.VERIFICATION_FAILED;
            
            return ResponseEntity.ok(ResponseDTO.of(response, responseCode));
        } catch (Exception e) {
            log.error("인증번호 확인 실패: {}", e.getMessage(), e);
            
            ResponseDTO<VerifyCodeResponseDTO> errorResponse = 
                ResponseDTO.of(null, ResponseCode.VERIFICATION_ERROR);
            errorResponse.setDetail(e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * 회원가입 완료
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/signup")
    public ResponseEntity<ResponseDTO<Void>> signup(@RequestBody @Valid SignupRequestDTO request) {
        log.debug("회원가입 요청: {}", request.getUserId());
        
        try {
            signupService.registerUser(request);
            
            log.debug("회원가입 성공: {}", request.getUserId());
            
            return ResponseEntity.ok(ResponseDTO.of(null, ResponseCode.SUCCESS_CODE));
        } catch (Exception e) {
            log.error("회원가입 실패: {}", e.getMessage(), e);
            
            ResponseDTO<Void> errorResponse = 
                ResponseDTO.of(null, ResponseCode.SIGNUP_FAILURE);
            errorResponse.setDetail(e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}