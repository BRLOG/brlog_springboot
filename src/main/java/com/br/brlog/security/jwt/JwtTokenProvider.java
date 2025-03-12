package com.br.brlog.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long tokenValidityInMilliseconds;

    private Key key;
    
    @PostConstruct
    public void init() {
        log.debug("JWT 토큰 제공자 초기화");
        // 시크릿 키를 Base64로 디코딩하여 키 생성
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        log.debug("JWT 시크릿 키 초기화 완료");
    }

    // 토큰 생성 (이메일과 권한 지정)
    public String createToken(String email, Collection<? extends GrantedAuthority> authorities) {
        log.debug("사용자 ID와 권한으로 토큰 생성: {}", email);
        
        String authoritiesString = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date validity = new Date(now + this.tokenValidityInMilliseconds);

        String token = Jwts.builder()
                .setSubject(email)
                .claim("auth", authoritiesString)
                .setIssuedAt(new Date(now))
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
        
        log.debug("JWT 토큰 생성 완료");
        return token;
    }

    // 인증 객체로부터 토큰 생성
    public String createToken(Authentication authentication) {
        log.debug("인증 객체로부터 토큰 생성: {}", authentication.getName());
        
        String username;
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        
        // OAuth2 인증인 경우
        if (authentication.getPrincipal() instanceof DefaultOAuth2User) {
            DefaultOAuth2User oauth2User = (DefaultOAuth2User) authentication.getPrincipal();
            // OAuth2UserService에서 설정한 키 사용
            username = oauth2User.getAttribute("userId");
            log.debug("OAuth2 사용자 확인: {}", username);
        } else {
            // 일반 로그인인 경우
            if (authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                username = userDetails.getUsername();
            } else {
                username = authentication.getName();
            }
            log.debug("일반 사용자 확인: {}", username);
        }
        
        // 토큰 생성 로직
        long now = (new Date()).getTime();
        Date validity = new Date(now + this.tokenValidityInMilliseconds);

        String authoritiesString = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        String token = Jwts.builder()
                .setSubject(username)
                .claim("auth", authoritiesString)
                .setIssuedAt(new Date(now))
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
        
        // 토큰 검증 테스트
        try {
            boolean isValid = validateToken(token);
            log.debug("생성된 토큰 검증 결과: {}", isValid);
        } catch (Exception e) {
            log.error("토큰 검증 테스트 중 오류 발생: {}", e.getMessage());
        }
        
        return token;
    }

    // 토큰으로부터 인증 정보 추출
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
                
        String authString = claims.get("auth", String.class);
        Collection<? extends GrantedAuthority> authorities;
        
        if (authString != null && !authString.isEmpty()) {
            authorities = Arrays.stream(authString.split(","))
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        } else {
            // 권한 정보가 없는 경우 기본 권한 부여
            authorities = Arrays.asList(new SimpleGrantedAuthority("ROLE_USER"));
        }

        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    // 토큰 검증
    public boolean validateToken(String token) {
        try {
            log.debug("토큰 검증 시도: {}", token.substring(0, Math.min(10, token.length())));
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            log.debug("토큰 검증 성공");
            return true;
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰: {}", e.getMessage());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("유효하지 않은 JWT 토큰: {}", e.getMessage());
            return false;
        }
    }

    // 토큰에서 이메일 추출
    public String getUsername(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            log.error("토큰에서 사용자명 추출 실패: {}", e.getMessage());
            return null;
        }
    }
    
    // 토큰 만료일 가져오기
    public Date getExpirationDate(String token) {
        try {
            log.debug("토큰 만료일 확인 시도: {}", token.substring(0, Math.min(10, token.length())));
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            log.debug("토큰 만료일 확인 성공");
            return claims.getExpiration();
        } catch (Exception e) {
            log.error("토큰 만료일 확인 실패: {}", e.getMessage());
            throw e;
        }
    }
    
    // 토큰 ID 생성 (Redis 키로 사용)
    public String getTokenId(String token) {
        try {
            // 토큰에서 사용자 이름 추출
            String username = getUsername(token);
            return username != null ? "token:" + username : "unknown";
        } catch (Exception e) {
            log.error("토큰 ID 생성 실패: {}", e.getMessage());
            return "token:" + System.currentTimeMillis();  // 고유 ID 생성
        }
    }
}