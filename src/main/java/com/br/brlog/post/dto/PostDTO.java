package com.br.brlog.post.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class PostDTO {
    private Long postId;
    private String categoryId;
    private String userId;
    private String title;
    private String content;
    private int viewCnt;
    private int likeCnt;
    private int commentCnt;
    private boolean isNotice;
    private String status;
    private String regDt;
    private String modDt;
    
    private String userNm;
    private String profileImgUrl;
}