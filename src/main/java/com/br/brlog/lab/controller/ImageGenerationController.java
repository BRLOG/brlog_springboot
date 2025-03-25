package com.br.brlog.lab.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.br.brlog.common.ResponseCode;
import com.br.brlog.common.dto.ResponseDTO;
import com.br.brlog.lab.service.ImageGenerationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/lab/image")
public class ImageGenerationController {
    
    private final ImageGenerationService imageGenerationService;
    
    /**
     * 이미지 생성 API - 텍스트만 사용하거나 참조 이미지도 함께 사용
     */
    @SuppressWarnings("unchecked")
	@PostMapping("/generate")
    public ResponseEntity<ResponseDTO<Map<String, String>>> generateImage(
            @RequestPart("prompt") String prompt,
            @RequestPart(value = "image", required = false) MultipartFile referenceImage,
            @RequestPart(value = "strength", required = false) String strengthStr) {
        
        if (prompt == null || prompt.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                ResponseDTO.of(null, ResponseCode.ERROR_IMAGE_EXPLAIN)
            );
        }
        
        try {
            String imageUrl;
            
            // 참조 이미지 여부에 따라 다른 메서드 호출
            if (referenceImage != null && !referenceImage.isEmpty()) {
                float strength = 0.7f; // 기본값
                if (strengthStr != null && !strengthStr.isEmpty()) {
                    try {
                        strength = Float.parseFloat(strengthStr);
                    } catch (NumberFormatException e) {
                        log.warn("강도 값 파싱 오류, 기본값 사용: {}", strengthStr);
                    }
                }
                
                log.info("참조 이미지 기반 생성: 프롬프트={}, 이미지 크기={}, 강도={}", 
                        prompt, referenceImage.getSize(), strength);
                imageUrl = imageGenerationService.generateImageWithReference(prompt, referenceImage, strength);
            } else {
                log.info("텍스트 기반 이미지 생성: 프롬프트={}", prompt);
                imageUrl = imageGenerationService.generateImage(prompt);
            }
            
            Map<String, String> result = new HashMap<>();
            result.put("imageUrl", imageUrl);
            
            return ResponseEntity.ok(ResponseDTO.from(result));
        } catch (Exception e) {
            log.error("이미지 생성 중 오류 발생", e);
            return ResponseEntity.status(500).body(
                ResponseDTO.of(null, ResponseCode.ERROR_IMAGE_GENERATE)
            );
        }
    }
}