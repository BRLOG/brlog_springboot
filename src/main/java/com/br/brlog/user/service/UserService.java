package com.br.brlog.user.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.br.brlog.auth.dto.SignupRequestDTO;
import com.br.brlog.redis.RedisService;
import com.br.brlog.security.jwt.JwtTokenProvider;
import com.br.brlog.user.dao.UserDAO;
import com.br.brlog.user.dto.LoginRequestDTO;
import com.br.brlog.user.dto.TokenResponseDTO;
import com.br.brlog.user.dto.UserDTO;
import com.br.brlog.user.dto.UserResponseDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
	private final UserDAO userDAO;
	private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RedisService redisService;

	/**
     * 사용자 목록 조회
     */
	public List<UserDTO> selectUserList(UserDTO dto) throws Exception {

		List<UserDTO> resList = userDAO.selectUserList(dto);

		return resList;
	}
	
	/**
     * 사용자 로그인 - AuthenticationManager 사용하지 않고 직접 인증
     */
    public TokenResponseDTO login(LoginRequestDTO loginRequest) throws Exception {
        log.info("로그인 요청 처리: {}", loginRequest.getUserId());
        
        try {
            // 1. 사용자 정보 조회
            UserDTO userDTO = userDAO.findByUserId(loginRequest.getUserId());
            if(userDTO == null) {
                log.error("사용자를 찾을 수 없음: {}", loginRequest.getUserId());
                throw new RuntimeException("유효하지 않은 이메일 또는 비밀번호");
            }
            
            // 2. 비밀번호 직접 검증
            //if (!passwordEncoder.matches(loginRequest.getUserPw(), userDTO.getUserPw())) {
            if(!loginRequest.getUserPw().equals(userDTO.getUserPw())) {
                log.error("비밀번호 불일치: {}", loginRequest.getUserId());
                throw new RuntimeException("유효하지 않은 이메일 또는 비밀번호");
            }
            
            log.info("비밀번호 검증 성공: {}", loginRequest.getUserId());
            
            // 3. 사용자 권한 설정
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            
            // 4. JWT 토큰 생성 - 사용자 ID와 권한으로 직접 생성
            String jwt = jwtTokenProvider.createToken(userDTO.getUserId(), authorities);
            Date expirationDate = jwtTokenProvider.getExpirationDate(jwt);
            long expirationTime = expirationDate.getTime() - System.currentTimeMillis();
            
            // 5. Redis에 토큰 저장
            String tokenKey = jwtTokenProvider.getTokenId(jwt);
            redisService.saveDataWithExpiration(tokenKey, jwt, expirationTime, TimeUnit.MILLISECONDS);
            
            // 6. 마지막 로그인 시간 업데이트
            userDAO.updateUser(userDTO);
            
            // 7. 응답 생성
            UserResponseDTO userResponse = UserDTO.toUserResponse(userDTO);
            
            return TokenResponseDTO.builder()
                    .token(jwt)
                    .user(userResponse)
                    .build();
            
        } catch (Exception e) {
            log.error("로그인 처리 중 오류 발생", e);
            throw e;
        }
    }
//	
//	/**
//     * 사용자 로그인
//     */
//    public TokenResponseDTO login(LoginRequestDTO loginRequest) throws Exception {
//        //log.debug("로그인 요청: {}", loginRequest.getEmail());
//        
//    	// 인증 객체 생성
//        UsernamePasswordAuthenticationToken authToken = 
//            new UsernamePasswordAuthenticationToken(loginRequest.getUserId(), loginRequest.getUserPw());
//    	
//        // AuthenticationManager를 통한 인증
//        Authentication authentication = authenticationManager.authenticate(authToken);
//        
//        // JWT 토큰 생성
//        String jwt = jwtTokenProvider.createToken(authentication);
//        Date expirationDate = jwtTokenProvider.getExpirationDate(jwt);
//        long expirationTime = expirationDate.getTime() - System.currentTimeMillis();
//        
//        // Redis에 토큰 저장
//        String tokenKey = jwtTokenProvider.getTokenId(jwt);
//        redisService.saveDataWithExpiration(tokenKey, jwt, expirationTime, TimeUnit.MILLISECONDS);
//        
//        // 사용자 정보 조회
//        UserDTO userDTO = userDAO.findByUserId(loginRequest.getUserId());
//        if (userDTO == null) {
//            throw new RuntimeException("유효하지 않은 이메일 또는 비밀번호");
//        }
//        
//        // 마지막 로그인 시간 업데이트
//        //userDTO.setLastLoginDt(LocalDateTime.now());
//        userDAO.update(userDTO);
//        
//        // 응답 생성
//        UserResponseDTO userResponse = UserDTO.toUserResponse(userDTO);
//        
//        return TokenResponseDTO.builder()
//                .token(jwt)
//                .user(userResponse)
//                .build();
//    }
    
    /**
     * 사용자 회원가입
     */
    public UserResponseDTO signup(SignupRequestDTO signupRequestDTO) throws Exception {
        //log.debug("회원가입 요청: {}", signupRequest.getEmail());
        
        // 이메일 중복 확인
        UserDTO existingUser = userDAO.findByUserId(signupRequestDTO.getUserId());
        if (existingUser != null) {
            throw new RuntimeException("이미 사용 중인 이메일입니다");
        }
        
        // 사용자 생성
        UserDTO userDTO = UserDTO.builder()
                .userId(signupRequestDTO.getUserId())
                .userNm(signupRequestDTO.getUserNm())
                .userPw(passwordEncoder.encode(signupRequestDTO.getUserPw()))
                //.verified(false) // 이메일 인증 필요
                //.createdAt(LocalDateTime.now())
                //.updatedAt(LocalDateTime.now())
                //.roles(Arrays.asList("ROLE_USER"))
                .build();
        
        userDAO.saveUser(userDTO);
        //userDAO.saveRoles(userDTO.getUserId(), Arrays.asList("ROLE_USER"));
        
        // 응답 생성
        return UserDTO.toUserResponse(userDTO);
    }
    
    /**
     * 사용자 로그아웃
     */
    public void logout(String authHeader) {
        //log.debug("로그아웃 요청");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7);
            String username = jwtTokenProvider.getUsername(jwt);
            Date expirationDate = jwtTokenProvider.getExpirationDate(jwt);
            long expirationTime = expirationDate.getTime() - System.currentTimeMillis();
            
            // Redis에서 사용자 토큰 삭제
            String tokenKey = jwtTokenProvider.getTokenId(jwt);
            redisService.deleteData(tokenKey);
            
            // 블랙리스트에 토큰 추가 (유효기간까지)
            String blacklistKey = "blacklist:token:" + jwt;
            redisService.saveDataWithExpiration(blacklistKey, "true", expirationTime, TimeUnit.MILLISECONDS);
            
            log.debug("사용자 로그아웃 처리 완료: {}", username);
        }
    }
    
    /**
     * 현재 사용자 정보 조회
     */
    public UserResponseDTO getCurrentUser(String userId) throws Exception {
        //log.debug("현재 사용자 정보 요청: {}", userId);
        
        UserDTO userDTO = userDAO.findByUserId(userId);
        if (userDTO == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다");
        }
        
        return UserDTO.toUserResponse(userDTO);
    }
    
    /**
     * OAuth2 로그인 처리 (소셜 로그인)
     */
    public UserDTO processOAuth2Login(String provider, String providerId, String userId, String userNm, String picture) throws Exception {
        log.debug("소셜 로그인 처리: provider={}, email={}", provider, userId);
        
        // 사용자 조회
        UserDTO userDTO = userDAO.findByUserId(userId);
        
        if (userDTO == null) {
            // 제공자 ID로 조회
            userDTO = userDAO.findByProviderAndProviderId(provider, providerId);
            
            if (userDTO == null) {
                // 새 사용자 생성
                userDTO = UserDTO.builder()
                        .userId(userId)
                        .userNm(userNm)
                        .userPw(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .provider(provider)
                        .providerId(providerId)
                        .profileImgUrl(picture)
                        //.enabled(true)
                        //.verified(true) // 소셜 로그인은 이미 이메일 인증됨
                        //.createdAt(LocalDateTime.now())
                        //.updatedAt(LocalDateTime.now())
                        //.lastLoginAt(LocalDateTime.now())
                        //.roles(Arrays.asList("ROLE_USER"))
                        .build();
                
                userDAO.saveUser(userDTO);
                //userDAO.saveRoles(userDTO.getUserId(), Arrays.asList("ROLE_USER"));
                
                //log.debug("새 소셜 로그인 사용자 생성: {}", userId);
            }
        } else if (userDTO.getProvider() == null) {
            // 기존 계정을 소셜 계정으로 연동
            userDTO.setProvider(provider);
            userDTO.setProviderId(providerId);
            //userDTO.setUpdatedAt(LocalDateTime.now());
            //userDTO.setLastLoginAt(LocalDateTime.now());
            
            userDAO.updateUser(userDTO);
            //log.debug("기존 계정을 소셜 계정으로 연동: {}", userId);
        }
        
        // 마지막 로그인 시간 업데이트
        //userDTO.setLastLoginAt(LocalDateTime.now());
        userDAO.updateUser(userDTO);
        
        return userDTO;
    }
    
    /**
     * 사용자 프로필 정보 조회
     */
    public UserDTO getUserProfile(String userId) {
        log.info("사용자 프로필 정보 조회: {}", userId);
        
        UserDTO userDTO = userDAO.findByUserId(userId);
        if (userDTO == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다");
        }
        
        // 비밀번호는 API 응답에서 제외
        userDTO.setUserPw(null);
        
        return userDTO;
    }
    
    /**
     * 사용자 프로필 정보 업데이트
     */
    public UserDTO updateUserProfile(UserDTO userProfileDTO) {
        log.info("사용자 프로필 정보 업데이트: {}", userProfileDTO.getUserId());
        
        // 기존 사용자 정보 확인
        UserDTO existingUser = userDAO.findByUserId(userProfileDTO.getUserId());
        if (existingUser == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다");
        }
        
        // 변경 가능한 필드만 업데이트
        existingUser.setUserNm(userProfileDTO.getUserNm());
        existingUser.setProfileImgUrl(userProfileDTO.getProfileImgUrl());
        existingUser.setBio(userProfileDTO.getBio());
        
        // 데이터베이스 업데이트
        userDAO.updateUserProfile(existingUser);
        
        // 업데이트된 사용자 정보 반환
        UserDTO updatedUser = userDAO.findByUserId(userProfileDTO.getUserId());
        updatedUser.setUserPw(null); // 비밀번호는 응답에서 제외
        
        return updatedUser;
    }
    
    /**
     * 사용자 비밀번호 변경
     */
    public boolean updatePassword(String userId, String currentPassword, String newPassword) {
        log.info("사용자 비밀번호 변경: {}", userId);
        
        // 기존 사용자 정보 확인
        UserDTO userDTO = userDAO.findByUserId(userId);
        if (userDTO == null) {
            throw new RuntimeException("사용자를 찾을 수 없습니다");
        }
        
        // 현재 비밀번호 확인
        // 운영 환경에서는 암호화된 비밀번호를 비교해야 함
        // if (!passwordEncoder.matches(currentPassword, userDTO.getUserPw())) {
        if (!currentPassword.equals(userDTO.getUserPw())) {
            log.error("현재 비밀번호가 일치하지 않음: {}", userId);
            return false;
        }
        
        // 새 비밀번호 암호화 (실제 환경에서 필요)
        // String encodedPassword = passwordEncoder.encode(newPassword);
        
        // 암호화 없이 직접 저장 (기존 코드 패턴에 맞춤)
        userDTO.setUserPw(newPassword);
        
        // 비밀번호 업데이트
        userDAO.updateUserPassword(userDTO);
        
        log.info("비밀번호 변경 성공: {}", userId);
        return true;
    }
}
