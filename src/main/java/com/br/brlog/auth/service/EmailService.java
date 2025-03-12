package com.br.brlog.auth.service;

import com.br.brlog.auth.dto.SendVerificationResponseDTO;
import com.br.brlog.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final RedisService redisService;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    private static final int VERIFICATION_CODE_LENGTH = 6;
    private static final int VERIFICATION_CODE_EXPIRATION_MINUTES = 5;
    
    /**
     * 인증번호 이메일 발송
     */
    public SendVerificationResponseDTO sendVerificationEmail(String email, String name) throws MessagingException {
        log.debug("인증번호 이메일 발송 시작: {}", email);
        
        // 인증번호 생성
        String verificationCode = generateVerificationCode();
        
        // 고유 인증 ID 생성
        String verificationId = UUID.randomUUID().toString();
        
        // Redis에 인증번호 저장
        String verificationKey = getVerificationKey(verificationId);
        redisService.saveDataWithExpiration(
                verificationKey, 
                verificationCode, 
                VERIFICATION_CODE_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );
        
        // 인증 상태 추적용 키 저장 (이메일별)
        String emailVerificationKey = getEmailVerificationKey(email);
        redisService.saveDataWithExpiration(
                emailVerificationKey,
                "PENDING",
                VERIFICATION_CODE_EXPIRATION_MINUTES,
                TimeUnit.MINUTES
        );
        
        // 이메일 내용 생성
        String subject = "[BR Blog] 회원가입 인증번호";
        String content = createVerificationEmailContent(name, verificationCode);
        
        // 이메일 발송
        sendEmail(email, subject, content);
        
        log.debug("인증번호 이메일 발송 완료: {}", email);
        
        return new SendVerificationResponseDTO(verificationId, VERIFICATION_CODE_EXPIRATION_MINUTES);
    }
    
    /**
     * 인증번호 검증
     */
    public boolean verifyCode(String email, String verificationId, String inputCode) {
        log.debug("인증번호 검증 시작: {}, {}", email, verificationId);
        
        // Redis에서 저장된 인증번호 조회
        String verificationKey = getVerificationKey(verificationId);
        String storedCode = redisService.getStringData(verificationKey);
        
        if (storedCode == null) {
            log.debug("인증번호가 만료되었거나 존재하지 않음: {}", verificationId);
            return false;
        }
        
        // 인증번호 일치 여부 확인
        boolean isVerified = storedCode.equals(inputCode);
        
        if (isVerified) {
            log.debug("인증번호 일치: {}", email);
            
            // 인증 완료 상태로 변경
            String emailVerificationKey = getEmailVerificationKey(email);
            redisService.saveDataWithExpiration(
                    emailVerificationKey,
                    "VERIFIED",
                    60, // 인증 완료 후 60분 동안 유효
                    TimeUnit.MINUTES
            );
            
            // 기존 인증번호 삭제
            redisService.deleteData(verificationKey);
        } else {
            log.debug("인증번호 불일치: {}", email);
        }
        
        return isVerified;
    }
    
    /**
     * 이메일 인증 완료 여부 확인
     */
    public boolean isEmailVerified(String email) {
        String emailVerificationKey = getEmailVerificationKey(email);
        String status = redisService.getStringData(emailVerificationKey);
        
        return "VERIFIED".equals(status);
    }
    
    /**
     * 인증번호 생성
     */
    private String generateVerificationCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        
        for (int i = 0; i < VERIFICATION_CODE_LENGTH; i++) {
            code.append(random.nextInt(10)); // 0-9 사이 숫자
        }
        
        return code.toString();
    }
    
    /**
     * 인증 이메일 내용 생성
     */
    private String createVerificationEmailContent(String name, String verificationCode) {
        return "<div style='margin:20px;'>"
                + "<h1>안녕하세요, " + name + "님!</h1>"
                + "<p>BR Blog 회원가입을 위한 인증번호입니다.</p>"
                + "<p>아래 인증번호를 입력창에 입력해주세요.</p>"
                + "<div style='font-size:24px;padding:10px;background-color:#f8f9fa;border-radius:5px;display:inline-block;'>"
                + "<strong>" + verificationCode + "</strong>"
                + "</div>"
                + "<p>인증번호는 " + VERIFICATION_CODE_EXPIRATION_MINUTES + "분 동안 유효합니다.</p>"
                + "<p>감사합니다.<br>BR Blog 팀 드림</p>"
                + "</div>";
    }
    
    /**
     * 이메일 발송
     */
    private void sendEmail(String to, String subject, String content) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true); // true: HTML 형식 사용
        
        mailSender.send(message);
    }
    
    /**
     * Redis 인증번호 키 생성
     */
    private String getVerificationKey(String verificationId) {
        return "verification:" + verificationId;
    }
    
    /**
     * Redis 이메일 인증 상태 키 생성
     */
    private String getEmailVerificationKey(String email) {
        return "emailVerification:" + email;
    }
}