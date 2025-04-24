package com.br.brlog.redis;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.br.brlog.notification.dto.NotificationDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
@EnableRedisRepositories
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String redisHost;
    
    @Value("${spring.data.redis.port}")
    private int redisPort;
    
    @Value("${spring.data.redis.password:#{null}}")
    private String redisPassword;
    
    @Bean
    @Qualifier("standardRedisConnectionFactory")
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(redisHost, redisPort);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }
        return new LettuceConnectionFactory(redisConfig);
    }
    
    @Bean
    @Qualifier("reactiveRedisConnectionFactory")
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(redisHost, redisPort);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            redisConfig.setPassword(redisPassword);
        }
        return new LettuceConnectionFactory(redisConfig);
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            @Qualifier("standardRedisConnectionFactory") RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // 자바 8 날짜/시간 타입 지원
        return mapper;
    }
    
    /**
     * 알림용 ReactiveRedisTemplate 설정
     * Reactive 프로그래밍 모델을 지원하는 Redis 템플릿
     */
    @Bean
    public ReactiveRedisTemplate<String, NotificationDTO> reactiveRedisTemplate(
            @Qualifier("reactiveRedisConnectionFactory") ReactiveRedisConnectionFactory factory) {
        ObjectMapper objectMapper = objectMapper();
        
        Jackson2JsonRedisSerializer<NotificationDTO> valueSerializer = 
                new Jackson2JsonRedisSerializer<>(NotificationDTO.class);
        valueSerializer.setObjectMapper(objectMapper);
        
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        
        RedisSerializationContext.RedisSerializationContextBuilder<String, NotificationDTO> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);
        
        RedisSerializationContext<String, NotificationDTO> context = 
                builder.value(valueSerializer).build();
        
        return new ReactiveRedisTemplate<>(factory, context);
    }
    
    /**
     * 읽음 상태 ID 저장용 ReactiveRedisTemplate 설정
     * String 타입 값을 다루기 위한 템플릿
     */
    @Bean
    @Qualifier("reactiveStringRedisTemplate")
    public ReactiveRedisTemplate<String, String> reactiveStringRedisTemplate(
            @Qualifier("reactiveRedisConnectionFactory") ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        RedisSerializationContext<String, String> stringContext =
                RedisSerializationContext.<String, String>newSerializationContext(stringSerializer)
                        .value(stringSerializer)
                        .build();
        return new ReactiveRedisTemplate<>(factory, stringContext);
    }
}