package com.br.brlog.file.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;

/**
 * 파일 정보 DTO
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class FileDTO {
    private String originalFilename;  // 원본 파일명
    private String filename;          // 저장된 파일명 (UUID)
    private long fileSize;            // 파일 크기
    private String fileType;          // 파일 타입 (MIME)
    private String filePath;          // 서버 내 파일 경로
    private String fileUrl;           // 접근 가능한 URL
}