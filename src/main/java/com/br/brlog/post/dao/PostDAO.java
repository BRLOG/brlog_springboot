package com.br.brlog.post.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.br.brlog.post.dto.PostDTO;
import com.br.brlog.post.dto.CommentDTO;
import com.br.brlog.post.dto.CategoryDTO;
import com.br.brlog.post.dto.ContributorDTO;
import com.br.brlog.user.dto.UserDTO;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository("postDAO")
public class PostDAO {
    private final SqlSessionTemplate sqlSession;
    
    /**
     * 게시글 목록 조회
     */
    public List<PostDTO> getPosts(String categoryId, int offset, int size, String sortBy) {
        Map<String, Object> params = new HashMap<>();
        params.put("categoryId", categoryId);
        params.put("offset", offset);
        params.put("size", size);
        params.put("sortBy", sortBy);
        return sqlSession.selectList("post.getPosts", params);
    }

    /**
     * 게시글 상세 조회
     */
    public PostDTO getPost(Long postId) {
        return sqlSession.selectOne("post.getPost", postId);
    }

    /**
     * 게시글 저장
     */
    public void savePost(PostDTO post) {
        sqlSession.insert("post.savePost", post);
    }

    /**
     * 게시글 수정
     */
    public void updatePost(PostDTO post) {
        sqlSession.update("post.updatePost", post);
    }

    /**
     * 게시글 삭제 (상태 변경)
     */
    public void deletePost(Long postId) {
        sqlSession.update("post.deletePost", postId);
    }

    /**
     * 게시글 조회수 증가
     */
    public void incrementViewCount(Long postId) {
        sqlSession.update("post.incrementViewCount", postId);
    }

    /**
     * 게시글 좋아요 수 증가
     */
    public void incrementLikeCount(Long postId) {
        sqlSession.update("post.incrementLikeCount", postId);
    }

    /**
     * 게시글 좋아요 수 감소
     */
    public void decrementLikeCount(Long postId) {
        sqlSession.update("post.decrementLikeCount", postId);
    }

    /**
     * 게시글 댓글 수 증가
     */
    public void incrementCommentCount(Long postId) {
        sqlSession.update("post.incrementCommentCount", postId);
    }
    
    /**
     * 게시글 댓글 수 감소
     */
    public void decreaseCommentCount(Long postId) {
        sqlSession.update("post.decreaseCommentCount", postId);
    }
    
    /**
     * 게시글 좋아요 추가
     */
    public void addLike(Long postId, String userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("postId", postId);
        params.put("userId", userId);
        sqlSession.insert("post.addLike", params);
    }

    /**
     * 게시글 좋아요 삭제
     */
    public void removeLike(Long postId, String userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("postId", postId);
        params.put("userId", userId);
        sqlSession.delete("post.removeLike", params);
    }

    /**
     * 게시글 좋아요 여부 확인
     */
    public boolean checkLike(Long postId, String userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("postId", postId);
        params.put("userId", userId);
        return sqlSession.selectOne("post.checkLike", params);
    }

    /**
     * 댓글 목록 조회
     */
    public List<CommentDTO> getComments(Long postId) {
        return sqlSession.selectList("post.getComments", postId);
    }

    /**
     * 댓글 목록 페이징 조회
     */
    public List<CommentDTO> getCommentsWithPagination(Long postId, int offset, int size) {
        Map<String, Object> params = new HashMap<>();
        params.put("postId", postId);
        params.put("offset", offset);
        params.put("size", size);
        return sqlSession.selectList("post.getCommentsWithPagination", params);
    }

    /**
     * 댓글 저장
     */
    public void saveComment(CommentDTO comment) {
        sqlSession.insert("post.saveComment", comment);
    }

    /**
     * 댓글 조회
     */
    public CommentDTO getComment(Long commentId) {
        return sqlSession.selectOne("post.getComment", commentId);
    }
    
    /**
     * 댓글 삭제
     */
    public void deleteComment(Long commentId) {
    	sqlSession.delete("post.deleteComment", commentId);
	}

    /**
     * 카테고리 목록 조회
     */
    public List<CategoryDTO> getCategories() {
        return sqlSession.selectList("post.getCategories");
    }

    /**
     * 기여자 목록 조회
     */
    public List<ContributorDTO> getContributors() {
        return sqlSession.selectList("post.getContributors");
    }

    /**
     * 게시글 검색
     */
    public List<PostDTO> searchPosts(String keyword, int offset, int size) {
        Map<String, Object> params = new HashMap<>();
        params.put("keyword", keyword);
        params.put("offset", offset);
        params.put("size", size);
        return sqlSession.selectList("post.searchPosts", params);
    }

    /**
     * 작성자 정보 조회
     */
    public UserDTO getAuthor(String userId) {
        return sqlSession.selectOne("post.getAuthor", userId);
    }
    
    /**
     * 게시글 좋아요 상태 확인
     */
    public boolean checkLikeStatus(Long postId, String userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("postId", postId);
        params.put("userId", userId);
        
        Integer count = sqlSession.selectOne("post.checkLikeStatus", params);
        return count != null && count > 0;
    }
}