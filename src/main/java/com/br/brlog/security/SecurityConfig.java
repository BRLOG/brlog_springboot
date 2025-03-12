package com.br.brlog.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.br.brlog.security.jwt.JwtFilter;
import com.br.brlog.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.br.brlog.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.br.brlog.security.oauth2.OAuth2UserService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final OAuth2UserService oauth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;
    
    @Value("${cors.allowed-methods}")
    private String allowedMethods;
    
    @Value("${cors.allowed-headers}")
    private String allowedHeaders;
    
    @Value("${cors.exposed-headers}")
    private String exposedHeaders;

    public SecurityConfig(JwtFilter jwtFilter, OAuth2UserService oauth2UserService,
                          OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
                          OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler) {
        this.jwtFilter = jwtFilter;
        this.oauth2UserService = oauth2UserService;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.oAuth2AuthenticationFailureHandler = oAuth2AuthenticationFailureHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
       http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                // 중요: /auth/login을 별도로 먼저 명시하여 인증 없이 접근 가능하게 함
                .requestMatchers("/auth/login", "/auth/signup").permitAll()
                // 그 다음 OAuth2 경로 정의
                .requestMatchers("/oauth2/**").permitAll()
                // 나머지 /auth/** 경로 정의
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/public/**").permitAll()
                //.requestMatchers(HttpMethod.GET, "/post/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/**").permitAll()
                
                // 인증 필요
                .requestMatchers(HttpMethod.POST, "/post/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/post/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/post/**").authenticated()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(endpoint -> endpoint
                    .baseUri("/oauth2/authorize")) // OAuth2 인증 시작 엔드포인트
                .redirectionEndpoint(endpoint -> endpoint
                    .baseUri("/oauth2/callback/*")) // OAuth2 콜백 엔드포인트
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oauth2UserService)) // OAuth2 사용자 서비스
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler)
            )
            // 리다이렉트 대신 401 Unauthorized 상태 코드 반환
            .exceptionHandling(exceptionHandling -> 
                exceptionHandling
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
    	CorsConfiguration configuration = new CorsConfiguration();
        
        // 환경 변수에서 허용된 출처 목록 가져오기 (쉼표로 구분된 문자열)
        List<String> allowedOriginsList = Arrays.asList(allowedOrigins.split(","));
        List<String> allowedMethodsList = Arrays.asList(allowedMethods.split(","));
        
        // 허용된 헤더에 X-Public-Request 추가
        String headersWithCustom = allowedHeaders + ",X-Public-Request";
        List<String> allowedHeadersList = Arrays.asList(headersWithCustom.split(","));
        
        List<String> exposedHeadersList = Arrays.asList(exposedHeaders.split(","));
        
        configuration.setAllowedOrigins(allowedOriginsList);
        configuration.setAllowedMethods(allowedMethodsList);
        configuration.setAllowedHeaders(allowedHeadersList);
        configuration.setExposedHeaders(exposedHeadersList);
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}