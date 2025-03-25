package com.br.brlog.lab.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.br.brlog.lab.dto.AIPostRequestDTO;
import com.br.brlog.lab.dto.AIPostResponseDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {
    
    private final RestTemplate restTemplate;
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;
    
    // 정확한 엔드포인트 업데이트
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-pro:generateContent";
    
    public AIPostResponseDTO generatePostDraft(AIPostRequestDTO requestDTO) {
        try {
            // API 요청 본문 구성
            Map<String, Object> requestBody = new HashMap<>();
            
            // 프롬프트 구성
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();
            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> part = new HashMap<>();
            
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("당신은 전문적인 블로그 작가입니다. 주어진 주제와 키워드를 바탕으로 정보가 풍부하고 구조적인 블로그 게시글 초안을 작성해 주세요.\n\n");
            promptBuilder.append("다음 주제와 키워드를 바탕으로 블로그 게시글 초안을 작성해주세요:\n\n");
            promptBuilder.append("주제: ").append(requestDTO.getTopic()).append("\n");
            
            if (requestDTO.getKeywords() != null && !requestDTO.getKeywords().isEmpty()) {
                promptBuilder.append("키워드: ").append(String.join(", ", requestDTO.getKeywords())).append("\n");
            }
            
            promptBuilder.append("\n블로그 제목을 첫 줄에 '# '로 시작하게 작성하고, 그 아래에는 마크다운 형식으로 구조적인 글을 작성해주세요.");
            
            part.put("text", promptBuilder.toString());
            parts.add(part);
            content.put("parts", parts);
            contents.add(content);
            requestBody.put("contents", contents);
            
            // 생성 설정
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 2048);
            requestBody.put("generationConfig", generationConfig);
            
            // API 요청 URL에 키 추가
            String apiUrlWithKey = GEMINI_API_URL + "?key=" + geminiApiKey;
            
            // 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // API 요청 전송
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            Map<String, Object> response = restTemplate.postForObject(apiUrlWithKey, request, Map.class);
            
            log.info("Gemini API 응답: {}", response);
            
            // 응답 파싱 (새로운 API 구조에 맞게 수정)
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> candidate = candidates.get(0);
            Map<String, Object> candidateContent = (Map<String, Object>) candidate.get("content");
            List<Map<String, Object>> candidateParts = (List<Map<String, Object>>) candidateContent.get("parts");
            String generatedText = (String) candidateParts.get(0).get("text");
            
            // 제목 추출
            String suggestedTitle = "";
            if (generatedText.startsWith("# ")) {
                int endOfFirstLine = generatedText.indexOf('\n');
                if (endOfFirstLine > 0) {
                    suggestedTitle = generatedText.substring(2, endOfFirstLine).trim();
                }
            }
            
            return AIPostResponseDTO.builder()
                    .content(generatedText)
                    .suggestedTitle(suggestedTitle)
                    .build();
            
        } catch (Exception e) {
            log.error("AI 게시글 초안 생성 중 오류 발생", e);
            log.error("오류 세부 정보: ", e);
            throw new RuntimeException("AI 게시글 초안 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}