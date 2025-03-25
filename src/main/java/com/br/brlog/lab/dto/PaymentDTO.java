package com.br.brlog.lab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDTO {
    // 결제 기본 정보
    private String paymentId;        // 결제 ID
    private String orderId;          // 주문 ID
    private String orderName;        // 주문명
    private Long amount;             // 결제 금액
    private String status;           // 결제 상태 (READY, IN_PROGRESS, DONE, CANCELED, FAILED)
    
    // 결제 요청자 정보
    private String userId;           // 사용자 ID
    private String customerName;     // 고객 이름
    
    // 결제 수단 정보
    private String method;           // 결제 수단 (카드, 가상계좌 등)
    private String paymentKey;       // 토스 결제 키
    
    // 카드 결제 정보
    private String cardNumber;       // 카드번호(마스킹)
    private String cardCompany;      // 카드사
    private Integer installmentPlanMonths; // 할부 개월 수
    
    // 시간 정보
    private String requestedAt;      // 결제 요청 시간
    private String approvedAt;       // 결제 승인 시간
    
    // 기타 정보
    private String receiptUrl;       // 영수증 URL
    private String failReason;       // 실패 사유
}