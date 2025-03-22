package com.br.brlog.lab.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.brlog.common.dto.ResponseDTO;
import com.br.brlog.lab.service.LabService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(value="/lab")
public class LabController {
    
    private final LabService labService;
    
    /**
     * GraphQL 쿼리 실행
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @PostMapping("/graphql")
    public ResponseEntity<ResponseDTO<Object>> executeGraphQL(
            @RequestBody Map<String, Object> request) throws Exception {
        
        String query = (String) request.get("query");
        Map<String, Object> variables = (Map<String, Object>) request.get("variables");
        
        Object result = labService.executeGraphQL(query, variables);
        ResponseDTO responseDTO = ResponseDTO.from(result);
        
        return ResponseEntity.ok(responseDTO);
    }
}