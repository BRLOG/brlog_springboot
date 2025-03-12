package com.br.brlog.security.oauth2;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.br.brlog.user.dao.UserDAO;
import com.br.brlog.user.dto.UserDTO;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;

    public OAuth2UserService(UserDAO userDAO, @Lazy PasswordEncoder passwordEncoder) {
        this.userDAO = userDAO;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        try {
            return processOAuth2User(userRequest, oauth2User);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("OAuth2 인증 중 예외 발생", ex);
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        // OAuth2 제공자 추출 (google, facebook 등)
        String provider = userRequest.getClientRegistration().getRegistrationId();
        
        // 사용자 정보 추출
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        // Google 로그인의 경우 처리
        String userId = null;
        String userNm = null;
        String providerId = null;
        
        if ("google".equals(provider)) {
        	userId = (String) attributes.get("email");
        	userNm = (String) attributes.get("name");
            providerId = (String) attributes.get("sub");
            log.debug("Google OAuth2 사용자 정보: email={}, name={}, providerId={}", userId, userNm, providerId);
        }
//        else if ("microsoft".equals(provider)) {
//            userId = (String) attributes.get("userPrincipalName");
//            userNm = (String) attributes.get("displayName");
//            providerId = (String) attributes.get("id");
//            log.debug("Microsoft OAuth2 사용자 정보: email={}, name={}, providerId={}", userId, userNm, providerId);
//        } else if ("apple".equals(provider)) {
//            // Apple은 다른 처리가 필요할 수 있음
//            userId = (String) attributes.get("email");
//            userNm = "Apple User"; // Apple은 이름을 제공하지 않을 수 있음
//            providerId = (String) attributes.get("sub");
//            log.debug("Apple OAuth2 사용자 정보: email={}, providerId={}", userId, providerId);
//        }
        
        if (userId == null) {
            log.error("OAuth2 제공자에서 이메일을 찾을 수 없습니다");
            throw new OAuth2AuthenticationException("이메일을 찾을 수 없습니다");
        }
        
        // 사용자 조회 또는 생성
        UserDTO user = userDAO.findByUserId(userId);
        
        if (user == null) {
            log.debug("새 OAuth2 사용자 생성: {}", userId);
            
            // 동일한 제공자 ID를 가진 사용자가 있는지 확인
            user = userDAO.findByProviderAndProviderId(provider, providerId);
            
            if (user == null) {
                // 새 사용자 생성
                user = UserDTO.builder()
                        .userId((String) attributes.get("email"))
                        .userNm(userNm)
                        // 랜덤 비밀번호 생성 (소셜 로그인이므로 실제로는 사용되지 않음)
                        //.userPw(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .provider(provider)
                        .providerId(providerId)
                        .profileImgUrl((String) attributes.get("picture"))
                        .userLevel("USER")
                        .enabledYn("Y")
                        .verifiedYn("Y") // 소셜 로그인은 이미 이메일 인증됨
                        //.createdAt(LocalDateTime.now())
                        //.updatedAt(LocalDateTime.now())
                        //.lastLoginAt(LocalDateTime.now())
                        //.roles(Arrays.asList("ROLE_USER"))
                        .build();
                
                userDAO.saveUser(user);
                //userDAO.saveRoles(user.getUserId(), Arrays.asList("ROLE_USER"));
            }
        } else {
            log.debug("기존 사용자 업데이트: {}", userId);
            
            // 기존 사용자 업데이트
            if (user.getProvider() == null) {
                // 소셜 연동 정보 업데이트
                user.setProvider(provider);
                user.setProviderId(providerId);
                //user.setUpdatedAt(LocalDateTime.now());
                //user.setLastLoginAt(LocalDateTime.now());
                userDAO.updateUser(user);
            }
        }
        
        // OAuth2User 생성 및 반환
        //Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        //user.getRoles().forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
        
        // 사용자 권한 설정 (USER_LEVEL 컬럼 사용)
        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + (user.getUserLevel() != null ? user.getUserLevel() : "USER")));
        
        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put("userId", user.getUserId());
        userAttributes.put("userNm", user.getUserNm());
        userAttributes.put("provider", provider);
        userAttributes.put("userLevel", user.getUserLevel());
        
        return new DefaultOAuth2User(
                authorities,
                userAttributes,
                "userId"
        );
    }
}