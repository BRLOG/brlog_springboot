package com.br.brlog.security.oauth2;

import com.br.brlog.redis.RedisService;
import com.br.brlog.security.jwt.JwtTokenProvider;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final RedisService redisService;
    
    @Value("${oauth2.authorizedRedirectUri}")
    private String redirectUri;

    public OAuth2AuthenticationSuccessHandler(JwtTokenProvider tokenProvider, RedisService redisService) {
        this.tokenProvider = tokenProvider;
        this.redisService = redisService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) 
            throws IOException, ServletException {
        log.debug("OAuth2 인증 성공: {}", authentication.getName());
        
        // JWT 토큰 생성
        String token = tokenProvider.createToken(authentication);
        
        // 만료 시간 계산
        Date expirationDate = tokenProvider.getExpirationDate(token);
        long expirationTime = expirationDate.getTime() - System.currentTimeMillis();
        
        // Redis에 토큰 저장
        String tokenKey = tokenProvider.getTokenId(token);
        redisService.saveDataWithExpiration(tokenKey, token, expirationTime, TimeUnit.MILLISECONDS);
        
        log.debug("토큰 생성 및 Redis 저장 완료");
        
        // 리다이렉트 URL에 토큰 추가
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", token)
                .build().toUriString();
        
        log.debug("리다이렉트 URL: {}", targetUrl);
        
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}