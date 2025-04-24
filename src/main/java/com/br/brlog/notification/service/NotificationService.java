package com.br.brlog.notification.service;

import java.time.LocalDateTime;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;

import com.br.brlog.notification.dto.NotificationDTO;
import com.br.brlog.notification.dto.NotificationDTO.NotificationType;
import com.br.brlog.post.dto.CommentDTO;
import com.br.brlog.post.dto.PostDTO;
import com.br.brlog.user.dto.UserDTO;
import com.br.brlog.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final KafkaSender<String, NotificationDTO> kafkaSender;
    private final String notificationTopic;
    private final UserService userService;
    
    // 실시간 알림 브로드캐스팅을 위한 Sinks 사용
    private final Many<NotificationDTO> notificationSink = Sinks.many().replay().latest();
    
    // Redis에 최근 알림 저장
    private final ReactiveRedisTemplate<String, NotificationDTO> redisTemplate;
    
    private final ReactiveRedisTemplate<String, String> redisStringTemplate;
    
    /**
     * 게시글 작성자에게 댓글 알림 전송
     */
    public Mono<Void> sendCommentNotification(CommentDTO comment, PostDTO post, UserDTO commenter) {
        // 댓글 작성자가 게시글 작성자와 같은 경우 알림 생략
        if (comment.getUserId().equals(post.getUserId())) {
            return Mono.empty();
        }
        
        String postAuthorId = post.getUserId();
        
        long tempId = System.currentTimeMillis();
        NotificationDTO notification = NotificationDTO.builder()
        		.id(tempId)
                .userId(postAuthorId)
                .senderUserId(comment.getUserId())
                .senderUserNm(commenter.getUserNm())
                .senderProfileImgUrl(commenter.getProfileImgUrl())
                .type(NotificationType.COMMENT)
                .content(createCommentNotificationContent(comment, post, commenter))
                .postId(post.getPostId())
                .commentId(comment.getCommentId())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        // 프로듀서 레코드 생성
        ProducerRecord<String, NotificationDTO> record = 
                new ProducerRecord<>(notificationTopic, postAuthorId, notification);
                
        // Kafka로 전송
        return kafkaSender.send(Mono.just(SenderRecord.create(record, notification.getUserId())))
                .doOnNext(result -> {
                    log.info("알림 전송 완료: {}", notification);
                    // 실시간 업데이트를 위해 sink로도 브로드캐스팅
                    notificationSink.tryEmitNext(notification);
                    
                    // Redis가 사용 가능한 경우 저장
                    if (redisTemplate != null) {
                        String key = "notifications:" + notification.getUserId();
                        redisTemplate.opsForList().leftPush(key, notification).subscribe();
                        // 리스트를 최근 100개만 유지하도록 정리
                        redisTemplate.opsForList().trim(key, 0, 99).subscribe();
                    }
                })
                .then();
    }
    
    /**
     * 댓글에 대한 알림 내용 생성
     */
    private String createCommentNotificationContent(CommentDTO comment, PostDTO post, UserDTO commenter) {
        return commenter.getUserNm() + "님이 회원님의 게시글에 댓글을 달았습니다: " + 
               (comment.getContent().length() > 30 
                ? comment.getContent().substring(0, 30) + "..." 
                : comment.getContent());
    }
    
    /**
     * 특정 사용자를 위한 알림 스트림 가져오기
     */
    public Flux<NotificationDTO> getNotificationsForUser(String userId) {
        return notificationSink.asFlux()
                .filter(notification -> notification.getUserId().equals(userId));
    }
    
    /**
     * 알림을 읽음 상태로 표시 (읽음 세트 사용)
     */
    public Mono<Void> markAsRead(Long notificationId, String userId) {
        log.info("알림을 읽음으로 표시: userId={}, notificationId={}", userId, notificationId);
        
        // 읽은 알림 세트 키
        String readSetKey = "notifications:read:" + userId;
        
        // notificationId를 읽음 세트에 추가
        return redisStringTemplate.opsForSet().add(readSetKey, notificationId.toString())
            .then(Mono.empty());
    }

    /**
     * 알림이 읽음 상태인지 확인
     */
    public Mono<Boolean> isRead(Long notificationId, String userId) {
        String readSetKey = "notifications:read:" + userId;
        return redisTemplate.opsForSet().isMember(readSetKey, notificationId.toString());
    }

    /**
     * Redis에서 최근 알림 가져오기 (읽음 상태 정보 포함)
     */
    public Flux<NotificationDTO> getRecentNotifications(String userId, int limit) {
    	log.debug("알림 조회 시작: userId={}, limit={}", userId, limit);
        
        if (redisTemplate == null) {
            log.warn("Redis 템플릿이 null입니다");
            return Flux.empty();
        }
        
        String key = "notifications:" + userId;
        String readSetKey = "notifications:read:" + userId;
        
        // 읽음 상태 세트 디버깅
        redisStringTemplate.opsForSet().members(readSetKey)
            .collectList()
            .subscribe(
                readIds -> log.debug("읽음 상태 목록: userId={}, readIds={}", userId, readIds),
                error -> log.error("읽음 상태 조회 오류: {}", error.getMessage())
            );
        
        // 읽지 않은 알림만 필터링
        return redisTemplate.opsForList().range(key, 0, -1)  // 모든 알림 가져오기
            .flatMap(notification -> {
                if (notification.getId() != null) {
                    return redisStringTemplate.opsForSet().isMember(readSetKey, notification.getId().toString())
                        .flatMap(isRead -> {
                            notification.setRead(isRead);
                            // 읽지 않은 알림만 반환
                            return isRead ? Mono.empty() : Mono.just(notification);
                        });
                }
                return Mono.just(notification);
            })
            .take(limit)  // limit 개수만큼만 가져오기
            .doOnComplete(() -> log.debug("읽지 않은 알림 조회 완료: userId={}", userId));
    }
}