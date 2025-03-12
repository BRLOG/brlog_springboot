package com.br.brlog.post.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.br.brlog.common.dto.ResponseDTO;
import com.br.brlog.post.dto.CategoryDTO;
import com.br.brlog.post.dto.CommentDTO;
import com.br.brlog.post.dto.ContributorDTO;
import com.br.brlog.post.dto.PostDTO;
import com.br.brlog.post.service.PostService;
import com.br.brlog.user.dto.UserDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(value="/post")
public class PostController {
    
    private final PostService postService;
    
    /**
     * 게시글 목록 조회
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @GetMapping("/list")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> getPosts(
            @RequestParam(required = false, name = "categoryId") String categoryId,
            @RequestParam(defaultValue = "0", name = "offset") int offset,
            @RequestParam(defaultValue = "10", name = "size") int size,
            @RequestParam(defaultValue = "regDt DESC", name = "sortBy") String sortBy) throws Exception {
        
        ResponseDTO responseDTO = ResponseDTO.from(
                postService.getPosts(categoryId, offset, size, sortBy));
        
        return ResponseEntity.ok(responseDTO);
    }
    
    /**
     * 게시글 상세 조회
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @GetMapping("/{postId}")
    public ResponseEntity<ResponseDTO<PostDTO>> getPost(@PathVariable Long postId) throws Exception {
        ResponseDTO responseDTO = ResponseDTO.from(postService.getPost(postId));
        return ResponseEntity.ok(responseDTO);
    }
    
    /**
     * 게시글 저장
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @PostMapping
    public ResponseEntity<ResponseDTO<PostDTO>> savePost(@RequestBody PostDTO post) throws Exception {
        ResponseDTO responseDTO = ResponseDTO.from(postService.savePost(post));
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }
    
    /**
     * 게시글 수정
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @PutMapping("/{postId}")
    public ResponseEntity<ResponseDTO<PostDTO>> updatePost(
            @PathVariable Long postId, 
            @RequestBody PostDTO post) throws Exception {
        post.setPostId(postId);
        ResponseDTO responseDTO = ResponseDTO.from(postService.updatePost(post));
        return ResponseEntity.ok(responseDTO);
    }
    
    /**
     * 게시글 삭제
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @DeleteMapping("/{postId}")
    public ResponseEntity<ResponseDTO<Void>> deletePost(@PathVariable Long postId) throws Exception {
        postService.deletePost(postId);
        ResponseDTO responseDTO = ResponseDTO.from(null);
        return ResponseEntity.ok(responseDTO);
    }
    
    /**
     * 게시글 좋아요 추가
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @PostMapping("/{postId}/like")
    public ResponseEntity<ResponseDTO<Boolean>> addLike(
            @PathVariable Long postId,
            @RequestParam String userId) throws Exception {
        
        boolean result = postService.addLike(postId, userId);
        ResponseDTO responseDTO = ResponseDTO.from(result);
        return ResponseEntity.ok(responseDTO);
    }
    
    /**
     * 게시글 좋아요 삭제
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @DeleteMapping("/{postId}/like")
    public ResponseEntity<ResponseDTO<Boolean>> removeLike(
            @PathVariable Long postId,
            @RequestParam String userId) throws Exception {
        
        boolean result = postService.removeLike(postId, userId);
        ResponseDTO responseDTO = ResponseDTO.from(result);
        return ResponseEntity.ok(responseDTO);
    }
    
    /**
     * 댓글 목록 조회
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @GetMapping("/{postId}/comments")
    public ResponseEntity<ResponseDTO<List<CommentDTO>>> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int size) throws Exception {
        
        ResponseDTO responseDTO = ResponseDTO.from(
                postService.getCommentsWithPagination(postId, offset, size));
        
        return ResponseEntity.ok(responseDTO);
    }
    
    /**
     * 댓글 저장
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @PostMapping("/{postId}/comments")
    public ResponseEntity<ResponseDTO<CommentDTO>> saveComment(
            @PathVariable Long postId,
            @RequestBody CommentDTO comment) throws Exception {
        
        comment.setPostId(postId);
        ResponseDTO responseDTO = ResponseDTO.from(postService.saveComment(comment));
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }
    
    /**
     * 카테고리 목록 조회
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @GetMapping("/categories")
    public ResponseEntity<ResponseDTO<List<CategoryDTO>>> getCategories() throws Exception {
        ResponseDTO responseDTO = ResponseDTO.from(postService.getCategories());
        return ResponseEntity.ok(responseDTO);
    }
    
    /**
     * 기여자 목록 조회
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @GetMapping("/contributors")
    public ResponseEntity<ResponseDTO<List<ContributorDTO>>> getContributors() throws Exception {
        ResponseDTO responseDTO = ResponseDTO.from(postService.getContributors());
        return ResponseEntity.ok(responseDTO);
    }
    
    /**
     * 게시글 검색
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @GetMapping("/search")
    public ResponseEntity<ResponseDTO<Map<String, Object>>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int size) throws Exception {
        
        ResponseDTO responseDTO = ResponseDTO.from(
                postService.searchPosts(keyword, offset, size));
        
        return ResponseEntity.ok(responseDTO);
    }
}