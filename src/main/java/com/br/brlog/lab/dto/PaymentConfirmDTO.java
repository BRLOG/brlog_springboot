package com.br.brlog.lab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmDTO {
    private String paymentKey;  // 결제 키
    private String orderId;     // 주문 ID
    private Long amount;        // 결제 금액
}