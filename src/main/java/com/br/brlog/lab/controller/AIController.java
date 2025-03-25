package com.br.brlog.lab.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.brlog.common.dto.ResponseDTO;
import com.br.brlog.lab.dto.AIPostRequestDTO;
import com.br.brlog.lab.dto.AIPostResponseDTO;
import com.br.brlog.lab.service.AIService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/lab/ai")
public class AIController {
    
    private final AIService aiService;
    
    @PostMapping("/generate-post")
    public ResponseEntity<ResponseDTO<AIPostResponseDTO>> generatePost(
            @RequestBody AIPostRequestDTO requestDTO) {
        
        log.info("AI 게시글 초안 생성 요청: 주제={}, 키워드={}", 
                requestDTO.getTopic(), requestDTO.getKeywords());
        
        AIPostResponseDTO responseDTO = aiService.generatePostDraft(requestDTO);
        
        return ResponseEntity.ok(ResponseDTO.from(responseDTO));
    }
}