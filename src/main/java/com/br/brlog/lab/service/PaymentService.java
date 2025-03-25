package com.br.brlog.lab.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.br.brlog.lab.dto.PaymentConfirmDTO;
import com.br.brlog.lab.dto.PaymentDTO;
import com.br.brlog.lab.dto.PaymentRequestDTO;
import com.br.brlog.user.dao.UserDAO;
import com.br.brlog.user.dto.UserDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserDAO userDAO;
    private final RestTemplate restTemplate;
    
    // 임시 저장소 (실제 구현에서는 DB 사용)
    private final Map<String, PaymentDTO> paymentStore = new ConcurrentHashMap<>();
    
    // 토스페이먼츠 API 키 (application.yml에서 설정)
    @Value("${toss.payments.secret-key}")
    private String tossPaymentsSecretKey;
    
    @Value("${toss.payments.success-url}")
    private String successUrl;
    
    @Value("${toss.payments.fail-url}")
    private String failUrl;
    
    private static final String TOSS_PAYMENTS_API_URL = "https://api.tosspayments.com/v1/payments";
    
    /**
     * 결제 요청 생성
     */
    public PaymentDTO createPaymentRequest(PaymentRequestDTO requestDTO) {
        try {
            // 사용자 정보 조회
            UserDTO user = userDAO.findByUserId(requestDTO.getUserId());
            if (user == null) {
                throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
            }
            
            // 상품 정보 추출
            Map<String, Object> paymentItem = requestDTO.getPaymentItem();
            String itemName = (String) paymentItem.get("name");
            String itemDescription = (String) paymentItem.get("description");
            Long amount = ((Number) paymentItem.get("amount")).longValue();
            
            // 현재 시간 포맷
            String requestedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
            
            // 결제 정보 생성
            PaymentDTO paymentDTO = PaymentDTO.builder()
                    .orderId(requestDTO.getOrderId())
                    .orderName(itemName)
                    .amount(amount)
                    .status("READY")
                    .userId(requestDTO.getUserId())
                    .customerName(user.getUserNm())
                    .requestedAt(requestedAt)
                    .build();
            
            // 임시 저장소에 저장 (실제 구현에서는 DB 사용)
            paymentStore.put(requestDTO.getOrderId(), paymentDTO);
            
            log.info("결제 요청 생성 완료: {}", paymentDTO.getOrderId());
            return paymentDTO;
            
        } catch (Exception e) {
            log.error("결제 요청 생성 중 오류 발생", e);
            throw new RuntimeException("결제 요청 생성 중 오류가 발생했습니다.", e);
        }
    }
    
    /**
     * 결제 승인 처리
     */
    public PaymentDTO confirmPayment(PaymentConfirmDTO confirmDTO) {
    	PaymentDTO paymentDTO = new PaymentDTO();
        try {
            log.info("결제 승인 처리 시작: {}", confirmDTO.getOrderId());
            
            // 기존 결제 정보 조회
            paymentDTO = paymentStore.get(confirmDTO.getOrderId());
            if (paymentDTO == null) {
                throw new IllegalArgumentException("존재하지 않는 결제 요청입니다.");
            }
            
            // 금액 검증
            if (!confirmDTO.getAmount().equals(paymentDTO.getAmount())) {
                log.warn("결제 금액 불일치: 요청={}, 원본={}", confirmDTO.getAmount(), paymentDTO.getAmount());
                throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
            }
            
            // 토스페이먼츠 API 호출을 위한 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString((tossPaymentsSecretKey + ":").getBytes()));
            
            // 요청 본문 생성
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("paymentKey", confirmDTO.getPaymentKey());
            requestBody.put("orderId", confirmDTO.getOrderId());
            requestBody.put("amount", confirmDTO.getAmount());
            
            // API 요청 및 응답 처리
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            Map<String, Object> response = restTemplate.postForObject(
                    TOSS_PAYMENTS_API_URL + "/" + confirmDTO.getPaymentKey(),
                    request,
                    Map.class);
            
            log.info("토스페이먼츠 결제 승인 응답: {}", response);
            
            // 응답 데이터 매핑
            String method = (String) response.get("method");
            String status = (String) response.get("status");
            String approvedAt = (String) response.get("approvedAt");
            String cardNumber = null;
            String cardCompany = null;
            Integer installmentPlanMonths = null;
            
            // 카드 결제인 경우 관련 정보 추출
            if ("card".equals(method) && response.containsKey("card")) {
                Map<String, Object> cardInfo = (Map<String, Object>) response.get("card");
                cardNumber = (String) cardInfo.get("number");
                cardCompany = (String) cardInfo.get("company");
                installmentPlanMonths = cardInfo.containsKey("installmentPlanMonths") ? 
                        ((Number) cardInfo.get("installmentPlanMonths")).intValue() : 0;
            }
            
            // 결제 정보 업데이트
            paymentDTO.setPaymentKey(confirmDTO.getPaymentKey());
            paymentDTO.setMethod(method);
            paymentDTO.setStatus(status);
            paymentDTO.setApprovedAt(approvedAt);
            paymentDTO.setCardNumber(cardNumber);
            paymentDTO.setCardCompany(cardCompany);
            paymentDTO.setInstallmentPlanMonths(installmentPlanMonths);
            
            // 영수증 URL 설정 (있는 경우)
            if (response.containsKey("receipt")) {
                paymentDTO.setReceiptUrl((String) response.get("receipt").toString());
            }
            
            // 임시 저장소 업데이트 (실제 구현에서는 DB 사용)
            paymentStore.put(confirmDTO.getOrderId(), paymentDTO);
            
            log.info("결제 승인 처리 완료: {}", paymentDTO.getOrderId());
            return paymentDTO;
            
        } catch (HttpClientErrorException e) {
        	// 이미 처리된 결제인 경우 기존 결제 정보 반환
            if (e.getResponseBodyAsString().contains("ALREADY_PROCESSED_PAYMENT")) {
                log.info("이미 처리된 결제입니다: {}", confirmDTO.getOrderId());
                
                // 기존에 저장된 결제 정보가 있으면 반환
                if (paymentDTO != null && paymentDTO.getPaymentKey() != null) {
                    return paymentDTO;
                }
                
                // 없으면 기본 정보만 설정하여 반환
                paymentDTO.setPaymentKey(confirmDTO.getPaymentKey());
                paymentDTO.setStatus("DONE");
                paymentDTO.setMethod("card");
                paymentDTO.setApprovedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
                return paymentDTO;
            }
            
            // 다른 오류는 그대로 던지기
            throw e;
        } catch (Exception e) {
            log.error("결제 승인 처리 중 오류 발생", e);
            throw new RuntimeException("결제 승인 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}