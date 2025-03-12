package com.br.brlog.user.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 토큰 응답 DTO
 */
@Data
@Builder
public class TokenResponseDTO {
    private String token;
    private UserResponseDTO user;
}