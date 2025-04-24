package com.br.brlog.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDTO {
    private Long id;
    private String userId;          	// 알림 받는사람 ID
    private String senderUserId;    	// 알림 보내는사람 ID
    private String senderUserNm;    	// 알림 보내는사람 이름
    private String senderProfileImgUrl; // 알림 보내는사람 profile url
    private NotificationType type;  	// 알림 타입 ENUM
    private String content;         	// 알림 내용
    private Long postId;            	// 게시글 ID
    private Long commentId;         	// 댓글 ID
    private boolean isRead;         	// 읽음 여부
    private LocalDateTime createdAt; 	// 생성일
    
    // Enum for notification types
    public enum NotificationType {
        COMMENT,       // New comment notification
        REPLY,         // Reply to comment
        LIKE,          // Post like
        SYSTEM         // System notification
    }
}