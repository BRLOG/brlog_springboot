package com.br.brlog.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
	private String userId;			// 사용자 ID
	private String userNm;			// 사용자 이름
	private String userPw;			// 사용자 비밀번호
	private String profileImgUrl;	// 사용자 프로필 URL
	private String bio;				// 사용자 자기소개

    // 소셜 로그인 정보
    private String provider;		// 소셜 로그인 제공자(ggoogle, apple, microsoft, ..)
    private String providerId;		// 소셜로그인 제공자의 사용자 ID
    
    private String userLevel;		// 사용자 권한(ADMIN, USER)
    private String enabledYn;     	// 계정 활성화 상태
    private String verifiedYn;     	// 이메일 인증 여부
    
    private String regDt;			// 등록일
    private String modDt;			// 수정일
    private String lastLoginDt;		// 마지막 로그인 일시
    private String lastLoginIp;		// 마지막 로그인 아이피
    
    /**
     * UserDTO를 UserResponseDTO로 변환
     */
    public static UserResponseDTO toUserResponse(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }
        
        return UserResponseDTO.builder()
                .userId(userDTO.getUserId())
                .userNm(userDTO.getUserNm())
                .profileImgUrl(userDTO.getProfileImgUrl())
                .provider(userDTO.getProvider())
                .build();
    }
}
