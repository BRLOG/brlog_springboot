package com.br.brlog.lab.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.br.brlog.post.dto.PostDTO;
import com.br.brlog.post.service.PostService;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabService {
    
    private final PostService postService;
    private GraphQL graphQL;
    
    @PostConstruct
    public void init() {
        try {
            // GraphQL 스키마 정의 - Mutation 추가
            String schema = """
                type Post {
                    postId: ID
                    categoryId: String
                    userId: String
                    title: String
                    content: String
                    viewCnt: Int
                    likeCnt: Int
                    commentCnt: Int
                    isNotice: Boolean
                    status: String
                    regDt: String
                    modDt: String
                    userNm: String
                }
                
                type PostsResponse {
                    posts: [Post]
                    totalCount: Int
                }
                
                type Query {
                    posts: PostsResponse
                }
                
                type Mutation {
                    createPost(title: String!, content: String!, categoryId: String!): Post
                }
            """;
            
            // 스키마 파싱
            TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(schema);
            
            // 리졸버 연결 - PostDTO를 Map으로 변환하여 반환
            RuntimeWiring runtimeWiring = RuntimeWiring.newRuntimeWiring()
                .type("Query", builder -> 
                    builder.dataFetcher("posts", environment -> {
                        Map<String, Object> result = postService.getPosts(null, 0, 10, "regDt DESC");
                        
                        // PostDTO 리스트를 Map으로 변환하여 클래스로더 문제 해결
                        List<PostDTO> postDTOList = (List<PostDTO>) result.get("posts");
                        List<Map<String, Object>> postMaps = postDTOList.stream()
                            .map(this::convertPostDTOToMap)
                            .collect(Collectors.toList());
                        
                        // 새로운 결과 맵 생성
                        Map<String, Object> newResult = new HashMap<>();
                        newResult.put("posts", postMaps);
                        newResult.put("totalCount", result.get("totalCount"));
                        
                        return newResult;
                    })
                )
                // Mutation 리졸버 추가
                .type("Mutation", builder ->
                    builder.dataFetcher("createPost", environment -> {
                        try {
                            // 인증된 사용자 정보 가져오기
                            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                            String userId = authentication.getName();
                            
                            // 인자 추출
                            String title = environment.getArgument("title");
                            String content = environment.getArgument("content");
                            String categoryId = environment.getArgument("categoryId");
                            
                            // DTO 생성
                            PostDTO post = new PostDTO();
                            post.setTitle(title);
                            post.setContent(content);
                            post.setCategoryId(categoryId);
                            post.setUserId(userId);
                            
                            // 저장
                            PostDTO savedPost = postService.savePost(post);
                            
                            // DTO를 Map으로 변환하여 반환 (클래스로더 문제 해결)
                            return convertPostDTOToMap(savedPost);
                        } catch (Exception e) {
                            log.error("게시글 생성 중 오류 발생", e);
                            throw e;
                        }
                    })
                )
                .build();
            
            // 스키마 생성
            GraphQLSchema graphQLSchema = new SchemaGenerator().makeExecutableSchema(typeRegistry, runtimeWiring);
            
            // GraphQL 엔진 초기화
            this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
            
            log.info("GraphQL 초기화 성공");
        } catch (Exception e) {
            log.error("GraphQL 초기화 실패", e);
        }
    }
    
    // 나머지 코드는 동일하게 유지...
    private Map<String, Object> convertPostDTOToMap(PostDTO post) {
        Map<String, Object> map = new HashMap<>();
        map.put("postId", post.getPostId());
        map.put("categoryId", post.getCategoryId());
        map.put("userId", post.getUserId());
        map.put("title", post.getTitle());
        map.put("content", post.getContent());
        map.put("viewCnt", post.getViewCnt());
        map.put("likeCnt", post.getLikeCnt());
        map.put("commentCnt", post.getCommentCnt());
        map.put("isNotice", post.isNotice());
        map.put("status", post.getStatus());
        map.put("regDt", post.getRegDt());
        map.put("modDt", post.getModDt());
        // userNm 필드가 있다면 추가
        if (post instanceof Map && ((Map<?,?>) post).containsKey("userNm")) {
            map.put("userNm", ((Map<?,?>) post).get("userNm"));
        }
        return map;
    }
    
    public Map<String, Object> executeGraphQL(String query, Map<String, Object> variables) {
        // 기존 코드와 동일하게 유지...
        try {
            if (graphQL == null) {
                log.error("GraphQL이 초기화되지 않았습니다");
                return Map.of("errors", List.of(Map.of("message", "GraphQL 서비스를 사용할 수 없습니다")));
            }
            
            // 쿼리 실행
            ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .variables(variables != null ? variables : Map.of())
                .build();
            
            ExecutionResult executionResult = graphQL.execute(executionInput);
            return executionResult.toSpecification();
        } catch (Exception e) {
            log.error("GraphQL 쿼리 실행 중 오류", e);
            Map<String, Object> error = new HashMap<>();
            error.put("errors", List.of(Map.of("message", e.getMessage())));
            return error;
        }
    }
}