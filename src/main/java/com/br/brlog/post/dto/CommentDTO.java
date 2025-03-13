package com.br.brlog.post.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class CommentDTO {
    private Long commentId;
    private Long postId;
    private String userId;
    private Long parentId;
    private String content;
    private String status;
    private String regDt;
    private String modDt;
    
    private String userNm;
    private String profileImgUrl;
}