package com.br.brlog.post.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.br.brlog.notification.service.NotificationService;
import com.br.brlog.post.dao.PostDAO;
import com.br.brlog.post.dto.CategoryDTO;
import com.br.brlog.post.dto.CommentDTO;
import com.br.brlog.post.dto.ContributorDTO;
import com.br.brlog.post.dto.PostDTO;
import com.br.brlog.user.dto.UserDTO;
import com.br.brlog.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {
    
    private final PostDAO postDAO;
    private final NotificationService notificationService;
    private final UserService userService;
    
    /**
     * 게시글 목록 조회
     */
    public Map<String, Object> getPosts(String categoryId, int offset, int size, String sortBy) {
        List<PostDTO> posts = postDAO.getPosts(categoryId, offset, size, sortBy);
        
        Map<String, Object> result = new HashMap<>();
        result.put("posts", posts);
        result.put("totalCount", posts.size()); // 실제로는 전체 갯수를 가져오는 별도 쿼리 필요
        
        return result;
    }
    
    /**
     * 게시글 상세 조회
     */
    @Transactional
    public PostDTO getPost(Long postId) {
        // 조회수 증가
        postDAO.incrementViewCount(postId);
        
        // 게시글 조회
        return postDAO.getPost(postId);
    }
    
    /**
     * 게시글 저장
     */
    @Transactional
    public PostDTO savePost(PostDTO post) {
    	// 현재 인증된 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName(); // 인증된 사용자의 ID
        
        // 사용자 ID 설정 BR
        post.setUserId(userId);
        
        postDAO.savePost(post);
        return post; // insert 후 생성된 ID가 post 객체에 설정됨
    }
    
    /**
     * 게시글 수정
     */
    @Transactional
    public PostDTO updatePost(PostDTO post) {
        postDAO.updatePost(post);
        return postDAO.getPost(post.getPostId());
    }
    
    /**
     * 게시글 삭제
     */
    @Transactional
    public void deletePost(Long postId) {
        postDAO.deletePost(postId);
    }
    
    /**
     * 게시글 좋아요 추가
     */
    @Transactional
    public boolean addLike(Long postId, String userId) {
        // 이미 좋아요가 있는지 확인
        if (postDAO.checkLike(postId, userId)) {
            return false;
        }
        
        postDAO.addLike(postId, userId);
        postDAO.incrementLikeCount(postId);
        return true;
    }
    
    /**
     * 게시글 좋아요 삭제
     */
    @Transactional
    public boolean removeLike(Long postId, String userId) {
        // 좋아요가 있는지 확인
        if (!postDAO.checkLike(postId, userId)) {
            return false;
        }
        
        postDAO.removeLike(postId, userId);
        postDAO.decrementLikeCount(postId);
        return true;
    }
    
    /**
     * 댓글 목록 조회
     */
    public List<CommentDTO> getComments(Long postId) {
        return postDAO.getComments(postId);
    }
    
    /**
     * 댓글 목록 페이징 조회
     */
    public List<CommentDTO> getCommentsWithPagination(Long postId, int offset, int size) {
        return postDAO.getCommentsWithPagination(postId, offset, size);
    }
    
    /**
     * 댓글 저장
     */
    @Transactional
    public CommentDTO saveComment(CommentDTO comment) {
    	// 1. 댓글 저장
        postDAO.saveComment(comment);
        postDAO.incrementCommentCount(comment.getPostId());
        
        // 2. 댓글 알림 처리 - 비동기적으로 처리하여 응답 시간에 영향 없도록 함
        Mono.fromRunnable(() -> {
            try {
                // 게시글 정보 조회
                PostDTO post = postDAO.getPost(comment.getPostId());
                
                // 댓글 작성자 정보 조회
                UserDTO commenter = userService.findByUserId(comment.getUserId());
                
                // 알림 전송
                notificationService.sendCommentNotification(comment, post, commenter)
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(
                        null,
                        error -> log.error("Failed to send notification: {}", error.getMessage())
                    );
            } catch (Exception e) {
                log.error("Error processing comment notification", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
        
        return comment; // insert 후 생성된 ID
    }
    
    /**
     * 댓글 삭제
     */
    public void deleteComment(Long commentId, Long postId) {
    	postDAO.deleteComment(commentId);
        postDAO.decreaseCommentCount(postId);
	}
    
    /**
     * 카테고리 목록 조회
     */
    public List<CategoryDTO> getCategories() {
        return postDAO.getCategories();
    }
    
    /**
     * 기여자 목록 조회
     */
    public List<ContributorDTO> getContributors() {
        return postDAO.getContributors();
    }
    
    /**
     * 게시글 검색
     */
    public Map<String, Object> searchPosts(String keyword, int offset, int size) {
        List<PostDTO> posts = postDAO.searchPosts(keyword, offset, size);
        
        Map<String, Object> result = new HashMap<>();
        result.put("posts", posts);
        result.put("totalCount", posts.size()); // 실제로는 전체 갯수를 가져오는 별도 쿼리 필요
        result.put("keyword", keyword);
        
        return result;
    }
    
    /**
     * 작성자 정보 조회
     */
    public UserDTO getAuthor(String userId) {
        return postDAO.getAuthor(userId);
    }
    
    /**
     * 게시글 좋아요 상태 확인
     */
    public boolean checkLikeStatus(Long postId, String userId) {
        return postDAO.checkLikeStatus(postId, userId);
    }
}