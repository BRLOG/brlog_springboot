package com.br.brlog.lab.service;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerationService {
    
    private final RestTemplate restTemplate;
    
    @Value("${stability.api.key}")
    private String stabilityApiKey;
    
    @Value("${stability.api.url}")
    private String stabilityApiUrl;
    
    @Value("${file.upload.dir}")
    private String fileUploadDir;
    
    @Value("${file.access.url}")
    private String fileAccessUrl;
    
    /**
     * 텍스트만으로 이미지 생성
     */
    public String generateImage(String prompt) {
        try {
            // 요청 본문 구성
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("text_prompts", List.of(Map.of(
                "text", prompt,
                "weight", 1
            )));
            requestBody.put("height", 512);
            requestBody.put("width", 512);
            requestBody.put("cfg_scale", 7);
            requestBody.put("steps", 30);
            
            // 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json"); // 추가
            headers.set("Authorization", "Bearer " + stabilityApiKey);
            
            // API 호출
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            Map response = restTemplate.postForObject(
                stabilityApiUrl + "/v1/generation/stable-diffusion-v1-6/text-to-image",
                entity,
                Map.class
            );
            
            // 응답 처리
            List<Map<String, Object>> artifacts = (List<Map<String, Object>>) response.get("artifacts");
            String base64Image = (String) artifacts.get(0).get("base64");
            
            // 파일 저장 및 URL 반환
            return saveAndGetImageUrl(base64Image);
            
        } catch (Exception e) {
            log.error("이미지 생성 중 오류 발생", e);
            throw new RuntimeException("이미지 생성 중 오류 발생: " + e.getMessage(), e);
        }
    }
    
    /**
     * 참조 이미지와 텍스트로 이미지 생성
     */
    public String generateImageWithReference(String prompt, MultipartFile referenceImage, float strength) {
        try {
        	// 이미지 리사이징 로직 추가
            byte[] resizedImageBytes = resizeImage(referenceImage.getBytes(), 1024, 1024);
            
            // multipart/form-data 요청 구성
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // 텍스트 프롬프트 추가 (URL 인코딩 유지)
            body.add("text_prompts[0][text]", URLEncoder.encode(prompt, "UTF-8"));
            body.add("text_prompts[0][weight]", "1");
            
            // 리사이징된 이미지 추가
            ByteArrayResource imageResource = new ByteArrayResource(resizedImageBytes) {
                @Override
                public String getFilename() {
                    return "image.png"; // 간단한 파일명 사용
                }
            };
            body.add("init_image", imageResource);
            
            // 기타 파라미터 추가
            body.add("image_strength", String.valueOf(strength));
            body.add("cfg_scale", "7");
            body.add("steps", "30");
            
            // 헤더 설정 - multipart/form-data로 변경
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Accept", "application/json");
            headers.set("Authorization", "Bearer " + stabilityApiKey);
            
            // API 호출
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            log.info("API URL: {}", stabilityApiUrl + "/v1/generation/stable-diffusion-v1-6/image-to-image");
            log.info("요청 헤더: {}", headers);
            
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                stabilityApiUrl + "/v1/generation/stable-diffusion-v1-6/image-to-image",
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
            
            Map response = responseEntity.getBody();
            
            // 응답 처리
            List<Map<String, Object>> artifacts = (List<Map<String, Object>>) response.get("artifacts");
            String generatedBase64 = (String) artifacts.get(0).get("base64");
            
            // 파일 저장 및 URL 반환
            return saveAndGetImageUrl(generatedBase64);
            
        } catch (Exception e) {
            log.error("이미지 생성 중 오류 발생", e);
            throw new RuntimeException("이미지 생성 중 오류 발생: " + e.getMessage(), e);
        }
    }
    
    /**
     * Base64 이미지를 파일로 저장하고 URL 반환
     */
    private String saveAndGetImageUrl(String base64Image) throws IOException {
        // 디렉토리 확인 및 생성
    	String uploadPath = fileUploadDir + File.separator + "ai_img" + File.separator;
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
         
        // 파일명 생성 및 저장
        String fileName = UUID.randomUUID().toString() + ".png";
        String filePath = uploadPath + File.separator + fileName;
        
        // Base64 디코딩 및 파일 저장
        byte[] decodedImage = Base64.getDecoder().decode(base64Image);
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(decodedImage);
        }
        
        // 접근 URL 반환
        return fileAccessUrl + "/ai_img/" + fileName;
    }
    
    /**
     * 이미지를 특정 크기로 리사이징
     */
    private byte[] resizeImage(byte[] imageBytes, int targetWidth, int targetHeight) throws IOException {
        // 원본 이미지 읽기
        InputStream is = new ByteArrayInputStream(imageBytes);
        BufferedImage originalImage = ImageIO.read(is);
        
        // 원본 이미지 크기
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        // 원본 비율 유지하면서 최대 크기 계산
        double ratio = Math.min(
            (double) targetWidth / originalWidth,
            (double) targetHeight / originalHeight
        );
        
        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);
        
        // 이미지 리사이징
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();
        
        // 리사이징된 이미지를 바이트 배열로 변환
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, "png", baos);
        return baos.toByteArray();
    }
}