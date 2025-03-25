package com.br.brlog.lab.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.brlog.common.dto.ResponseDTO;
import com.br.brlog.lab.dto.PaymentConfirmDTO;
import com.br.brlog.lab.dto.PaymentDTO;
import com.br.brlog.lab.dto.PaymentRequestDTO;
import com.br.brlog.lab.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/lab/payment")
public class PaymentController {

    private final PaymentService paymentService;
    
    /**
     * 결제 요청 생성
     */
    @PostMapping("/request")
    public ResponseEntity<ResponseDTO<PaymentDTO>> requestPayment(@RequestBody PaymentRequestDTO requestDTO) {
        log.info("결제 요청 생성: {}", requestDTO.getOrderId());
        
        // 현재 인증된 사용자 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserId = authentication.getName();
        
        // 요청한 사용자와 인증된 사용자가 일치하는지 확인
        if (!currentUserId.equals(requestDTO.getUserId())) {
            log.warn("결제 요청 사용자 불일치: 요청={}, 인증={}", requestDTO.getUserId(), currentUserId);
            throw new IllegalArgumentException("결제 요청 권한이 없습니다.");
        }
        
        // 결제 요청 생성
        PaymentDTO paymentDTO = paymentService.createPaymentRequest(requestDTO);
        ResponseDTO<PaymentDTO> responseDTO = ResponseDTO.from(paymentDTO);
        
        return ResponseEntity.ok(responseDTO);
    }
    
    /**
     * 결제 승인 요청
     */
    @PostMapping("/confirm")
    public ResponseEntity<ResponseDTO<PaymentDTO>> confirmPayment(@RequestBody PaymentConfirmDTO confirmDTO) {
        log.info("결제 승인 요청: {}", confirmDTO.getOrderId());
        
        // 결제 승인 처리
        PaymentDTO paymentDTO = paymentService.confirmPayment(confirmDTO);
        ResponseDTO<PaymentDTO> responseDTO = ResponseDTO.from(paymentDTO);
        
        return ResponseEntity.ok(responseDTO);
    }
}