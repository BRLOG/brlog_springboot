package com.br.brlog.notification.controller;

import java.util.Date;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.br.brlog.common.dto.ResponseDTO;
import com.br.brlog.notification.dto.NotificationDTO;
import com.br.brlog.notification.service.NotificationService;
import com.br.brlog.security.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtTokenProvider tokenProvider;
    
    /**
     * 실시간 알림을 위한 SSE(Server-Sent Events) 엔드 포인트
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<NotificationDTO> streamNotifications(
            @RequestParam("userId") String userId,
            @RequestParam(value = "token", required = false) String token) {
        
    	// 토큰 유효성 검사 (JWT)
    	if (token == null || !tokenProvider.validateToken(token)) {
            return Flux.error(new AccessDeniedException("Unauthorized"));
        }
        
        log.info("User {} connected to notification stream", userId);
        return notificationService.getNotificationsForUser(userId)
                .doOnNext(notification -> log.debug("Sending notification to user {}: {}", userId, notification))
                .doOnCancel(() -> log.info("User {} disconnected from notification stream", userId));
    }
    
    /**
     * 최근 알림 얻기
     */
    @SuppressWarnings("unchecked")
    @GetMapping
    public Mono<ResponseEntity<ResponseDTO<List<NotificationDTO>>>> getRecentNotifications(
            @RequestParam("userId") String userId,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        
//        Flux<NotificationDTO> notifications = notificationService.getRecentNotifications(userId, limit);
//        ResponseDTO<Flux<NotificationDTO>> responseDTO = ResponseDTO.from(notifications);
//        return Mono.just(ResponseEntity.ok(responseDTO));
    	
    	// Flux를 List로 변환 후 반환
        return notificationService.getRecentNotifications(userId, limit)
                .collectList()  // Flux를 Mono<List>로 변환
                .map(notifications -> {
                    ResponseDTO<List<NotificationDTO>> responseDTO = ResponseDTO.from(notifications);
                    return ResponseEntity.ok(responseDTO);
                });
    }
    
    /**
     * 알림 읽음 처리
     */
    @SuppressWarnings("unchecked")
    @PostMapping("/{notificationId}/read")
    public Mono<ResponseEntity<ResponseDTO<Void>>> markAsRead(
            @PathVariable("notificationId") Long notificationId,
            @RequestParam("userId") String userId,
            @RequestParam(value = "token", required = false) String tokenParam,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        // 디버깅: 받은 모든 값 로깅
        log.info("====== NOTIFICATION READ DEBUG ======");
        log.info("NotificationId: {}", notificationId);
        log.info("UserId: {}", userId);
        log.info("Token param length: {}", tokenParam != null ? tokenParam.length() : 0);
        log.info("Auth header present: {}", authHeader != null);
        
        boolean isAuthenticated = false;
        String usedToken = null;
        
        try {
            // 1. 인증 헤더 처리
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                usedToken = authHeader.substring(7);
                log.info("Auth header token first 20 chars: {}", usedToken.substring(0, Math.min(20, usedToken.length())));
                log.info("Validating from auth header");
                isAuthenticated = tokenProvider.validateToken(usedToken);
                log.info("Auth header validation result: {}", isAuthenticated);
                
                if (!isAuthenticated) {
                    try {
                        // 만료 날짜 확인
                        Date expDate = tokenProvider.getExpirationDate(usedToken);
                        log.info("Auth header token expiration: {}", expDate);
                    } catch (Exception e) {
                        log.error("Failed to get expiration date from auth header token: {}", e.getMessage());
                    }
                }
            }
            
            // 2. 토큰 파라미터 처리
            if (!isAuthenticated && tokenParam != null) {
                usedToken = tokenParam;
                log.info("Token param first 20 chars: {}", usedToken.substring(0, Math.min(20, usedToken.length())));
                log.info("Validating from token param");
                isAuthenticated = tokenProvider.validateToken(tokenParam);
                log.info("Token param validation result: {}", isAuthenticated);
                
                if (!isAuthenticated) {
                    try {
                        // 만료 날짜 확인
                        Date expDate = tokenProvider.getExpirationDate(tokenParam);
                        log.info("Token param expiration: {}", expDate);
                    } catch (Exception e) {
                        log.error("Failed to get expiration date from token param: {}", e.getMessage());
                    }
                }
            }
            
            // 3. 토큰 검증 실패 원인 분석
            if (!isAuthenticated && usedToken != null) {
                try {
                    // 사용자명 추출 시도
                    String username = tokenProvider.getUsername(usedToken);
                    log.info("Token username: {}", username);
                } catch (Exception e) {
                    log.error("Token username extraction error: {}", e.getMessage());
                }
            }
            
            if (!isAuthenticated) {
                log.warn("Authentication failed");
                return Mono.error(new AccessDeniedException("Unauthorized"));
            }
            
            log.info("Authentication successful!");
            return notificationService.markAsRead(notificationId, userId)
                    .doOnSuccess(v -> log.info("Notification {} marked as read", notificationId))
                    .thenReturn(ResponseEntity.ok(ResponseDTO.from(null)));
                    
        } catch (Exception e) {
            log.error("Unexpected error in markAsRead: {}", e.getMessage(), e);
            return Mono.error(e);
        }
    }
}