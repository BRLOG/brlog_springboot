package com.br.brlog.file.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.br.brlog.file.dto.FileDTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FileService {
    
    @Value("${file.upload.dir}")
    private String uploadDir;
    
    @Value("${file.access.url}")
    private String fileAccessUrl;
    
    /**
     * 파일 업로드 처리
     */
    public FileDTO uploadFile(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }
        
        // 파일 확장자 검증
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getFileExtension(originalFilename);
        if (!isValidFileExtension(extension)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다: " + extension);
        }
        
        // 저장 디렉토리 설정 (yyyy/MM/dd 형식)
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path uploadPath = Paths.get(uploadDir + "/post", dateDir);
        
        // 디렉토리가 없으면 생성
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // UUID를 이용한 고유 파일명 생성
        String uniqueFilename = UUID.randomUUID().toString() + "." + extension;
        
        // 파일 저장
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath);
        
        // 파일 접근 URL 생성
        String fileUrl = fileAccessUrl + "/post/" + dateDir + "/" + uniqueFilename;
        
        log.info("파일 업로드 완료: {}", fileUrl);
        
        // 파일 정보 DTO 생성
        FileDTO fileDTO = new FileDTO();
        fileDTO.setOriginalFilename(originalFilename);
        fileDTO.setFilename(uniqueFilename);
        fileDTO.setFileSize(file.getSize());
        fileDTO.setFileType(file.getContentType());
        fileDTO.setFilePath(filePath.toString());
        fileDTO.setFileUrl(fileUrl);
        
        return fileDTO;
    }
    
    /**
     * 프로필 이미지 업로드 처리
     */
    public FileDTO uploadProfileImage(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다.");
        }
        
        // 파일 확장자 검증 (이미지 파일만 허용)
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getFileExtension(originalFilename);
        if (!isValidImageExtension(extension)) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다: " + extension);
        }
        
        // 파일 크기 검증 (10MB 제한)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("이미지 크기는 10MB를 초과할 수 없습니다.");
        }
        
        // 프로필 이미지 저장 디렉토리 설정
        Path uploadPath = Paths.get(uploadDir + "/profile");
        
        // 디렉토리가 없으면 생성
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // UUID를 이용한 고유 파일명 생성
        String uniqueFilename = UUID.randomUUID().toString() + "." + extension;
        
        // 파일 저장
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath);
        
        // 파일 접근 URL 생성
        String fileUrl = fileAccessUrl + "/profile/" + uniqueFilename;
        
        log.info("프로필 이미지 업로드 완료: {}", fileUrl);
        
        // 파일 정보 DTO 생성
        FileDTO fileDTO = new FileDTO();
        fileDTO.setOriginalFilename(originalFilename);
        fileDTO.setFilename(uniqueFilename);
        fileDTO.setFileSize(file.getSize());
        fileDTO.setFileType(file.getContentType());
        fileDTO.setFilePath(filePath.toString());
        fileDTO.setFileUrl(fileUrl);
        
        return fileDTO;
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
    
    /**
     * 파일 확장자 검증
     */
    private boolean isValidFileExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        
        // 허용된 확장자 목록
        String[] allowedExtensions = {
            "jpg", "jpeg", "png", "gif", "webp", 
            "pdf", "doc", "docx", "xls", "xlsx", "txt"
        };
        
        for (String allowedExt : allowedExtensions) {
            if (allowedExt.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 이미지 파일 확장자 검증
     */
    private boolean isValidImageExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        
        // 허용된 이미지 확장자 목록
        String[] allowedImageExtensions = {"jpg", "jpeg", "png", "gif", "webp"};
        
        return Arrays.stream(allowedImageExtensions)
                .anyMatch(ext -> ext.equalsIgnoreCase(extension));
    }
}
