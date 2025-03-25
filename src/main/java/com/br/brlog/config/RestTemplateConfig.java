package com.br.brlog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    
	@Bean
    public RestTemplate restTemplate() {
//        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
//        factory.setConnectTimeout(10000); // 10초
//        factory.setReadTimeout(30000);    // 30초
//        
//        return new RestTemplate(factory);
        
        return new RestTemplate();
    }
}