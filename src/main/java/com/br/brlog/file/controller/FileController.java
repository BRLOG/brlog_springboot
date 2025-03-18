package com.br.brlog.file.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.br.brlog.common.dto.ResponseDTO;
import com.br.brlog.file.dto.FileDTO;
import com.br.brlog.file.service.FileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value="/file")
public class FileController {
    
    private final FileService fileService;
    
    @Value("${file.upload.dir}")
    private String uploadDir;
    
    /**
     * 파일 업로드 API
     * 게시글 작성 중 즉시 파일 업로드를 처리합니다.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @PostMapping("/upload")
    public ResponseEntity<ResponseDTO<FileDTO>> uploadFile(
            @RequestParam("file") MultipartFile file) throws Exception {
        
        FileDTO fileDTO = fileService.uploadFile(file);
        ResponseDTO responseDTO = ResponseDTO.from(fileDTO);
        
        return ResponseEntity.ok(responseDTO);
    }
    
    /**
     * 프로필 이미지 업로드 API
     * 사용자 프로필 이미지 업로드를 처리합니다.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @PostMapping("/profile-image")
    public ResponseEntity<ResponseDTO<FileDTO>> uploadProfileImage(
            @RequestParam("file") MultipartFile file) throws Exception {
        
        FileDTO fileDTO = fileService.uploadProfileImage(file);
        ResponseDTO responseDTO = ResponseDTO.from(fileDTO);
        
        return ResponseEntity.ok(responseDTO);
    }
    
    /**
     * 프로필 이미지 조회 API
     * 업로드된 프로필 이미지 파일을 제공합니다.
     */
    @GetMapping("/profile/{filename:.+}")
    public ResponseEntity<Resource> getProfileImage(@PathVariable String filename) {
        try {
            // 파일 경로 생성
            Path filePath = Paths.get(uploadDir + "/profile").resolve(filename).normalize();
            
            // URL 리소스로 변환
            Resource resource = new UrlResource(filePath.toUri());
            
            // 파일 존재 여부 확인
            if (!resource.exists()) {
                log.error("프로필 이미지를 찾을 수 없습니다: {}", filename);
                return ResponseEntity.notFound().build();
            }
            
            // 파일 타입 결정
            String contentType = determineContentType(filename);
            
            // 응답 헤더 설정
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
            
        } catch (IOException e) {
            log.error("프로필 이미지 제공 중 오류 발생", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 파일 확장자에 따른 Content-Type 결정
     */
    private String determineContentType(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            default:
                return "application/octet-stream";
        }
    }
    
    /**
     * 파일 확장자 추출
     */
    private String getFileExtension(String filename) {
        if (filename.lastIndexOf(".") != -1 && filename.lastIndexOf(".") != 0) {
            return filename.substring(filename.lastIndexOf(".") + 1);
        } else {
            return "";
        }
    }
}