package com.br.brlog.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendVerificationResponseDTO {
    private String verificationId;
    private int expirationMinutes;
}
