package com.br.brlog.user.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 사용자 응답 DTO
 */
@Data
@Builder
public class UserResponseDTO {
    private String userId;
    private String userNm;
    private String profileImgUrl;
    private String provider;
}