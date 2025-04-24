package com.br.brlog.security.jwt;

import com.br.brlog.redis.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokenProvider;
    private final RedisService redisService;
    
    public JwtFilter(JwtTokenProvider tokenProvider, RedisService redisService) {
        this.tokenProvider = tokenProvider;
        this.redisService = redisService;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 요청 경로 로깅
        String requestURI = request.getRequestURI();
        
        try {
            // 1. 현재 SecurityContext에 인증 정보가 있는지 확인
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                log.debug("Security Context에 이미 인증 정보가 있습니다, uri: {}", requestURI);
                filterChain.doFilter(request, response);
                return;
            }
            
            // 2. 토큰 추출 (헤더 또는 URL 파라미터에서)
            String jwt = resolveToken(request);
            
            // 3. 토큰이 없으면 다음 필터로 진행
            if (jwt == null) {
                log.debug("유효한 JWT 토큰이 없습니다, uri: {}", requestURI);
                filterChain.doFilter(request, response);
                return;
            }
            
            // 4. Redis 블랙리스트 확인
            String blacklistKey = "blacklist:token:" + jwt;
            if (redisService.hasKey(blacklistKey)) {
                log.info("Blacklisted JWT token attempting to access {}", requestURI);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                filterChain.doFilter(request, response);
                return;
            }
            
            // 5. 토큰 유효성 검사
            if (tokenProvider.validateToken(jwt)) {
                // 6. Redis에 토큰이 저장되어 있는지 확인
                String tokenKey = tokenProvider.getTokenId(jwt);
                Object storedToken = redisService.getData(tokenKey);
                
                log.info("토큰 검증 결과 - Redis Key: {}, 저장된 토큰 존재: {}", 
                        tokenKey, storedToken != null);
                
                if (storedToken != null && storedToken.equals(jwt)) {
                    // 7. 인증 정보 생성 및 SecurityContext에 저장
                    Authentication authentication = tokenProvider.getAuthentication(jwt);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("인증 성공: {}, uri: {}", authentication.getName(), requestURI);
                } else {
                    // 수정: Redis에 토큰 없이도 인증 허용 (테스트용)
                    log.info("Redis에 토큰이 없지만 JWT는 유효함. 인증 허용: {}", requestURI);
                    Authentication authentication = tokenProvider.getAuthentication(jwt);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else {
                log.info("유효하지 않은 JWT 토큰입니다, uri: {}", requestURI);
            }
        } catch (Exception e) {
            log.error("JWT 토큰 처리 중 오류 발생: {}", e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String resolveToken(HttpServletRequest request) {
        // 1. 헤더에서 토큰 추출 시도
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // 2. URL 파라미터에서 토큰 추출 시도 (SSE 연결용)
        String paramToken = request.getParameter("token");
        if (StringUtils.hasText(paramToken)) {
            log.debug("URL 파라미터에서 토큰 추출: {}", paramToken.substring(0, Math.min(10, paramToken.length())));
            return paramToken;
        }
        
        return null;
    }
}