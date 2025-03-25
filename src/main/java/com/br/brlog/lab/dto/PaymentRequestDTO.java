package com.br.brlog.lab.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO {
    private String orderId;                  // 주문 ID
    private Map<String, Object> paymentItem; // 결제할 상품 정보
    private String userId;                   // 사용자 ID
}