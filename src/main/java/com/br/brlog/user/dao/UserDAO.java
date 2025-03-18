package com.br.brlog.user.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.br.brlog.user.dto.UserDTO;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository("userDAO")
public class UserDAO {
	//private final SqlSession sqlSession;
	private final SqlSessionTemplate sqlSession;
	
	public List<UserDTO> selectUserList(UserDTO dto) {
		return sqlSession.selectList("user.selectUserList", dto);
	}
	
	/**
     * 사용자 조회
     */
    public UserDTO findByUserId(String userId) {
        //log.debug("findByEmail: {}", email);
        return sqlSession.selectOne("user.findByUserId", userId);
    }
    
    /**
     * 소셜 로그인 제공자와 ID로 사용자 조회
     */
    public UserDTO findByProviderAndProviderId(String provider, String providerId) {
        //log.debug("findByProviderAndProviderId: provider={}, providerId={}", provider, providerId);
        Map<String, Object> params = new HashMap<>();
        params.put("provider", provider);
        params.put("providerId", providerId);
        return sqlSession.selectOne("user.findByProviderAndProviderId", params);
    }
    
    /**
     * 사용자 저장
     */
    public void saveUser(UserDTO userDTO) {
        //log.debug("save: {}", userDTO);
        sqlSession.insert("user.saveUser", userDTO);
    }
    
    /**
     * 사용자 정보 업데이트
     */
    public void updateUser(UserDTO userDTO) {
        //log.debug("update: {}", userDTO);
        sqlSession.update("user.updateUser", userDTO);
    }
    
    /**
     * 사용자 권한 저장
     */
    public void saveRoles(Long userId, List<String> roles) {
        //log.debug("saveRoles: userId={}, roles={}", userId, roles);
        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("roles", roles);
        sqlSession.insert("user.saveRoles", params);
    }
    
    /**
     * 사용자 프로필 정보 업데이트
     */
    public void updateUserProfile(UserDTO userDTO) {
        //log.debug("updateUserProfile: {}", userDTO);
        sqlSession.update("user.updateUserProfile", userDTO);
    }
    
    /**
     * 사용자 비밀번호 업데이트
     */
    public void updateUserPassword(UserDTO userDTO) {
        //log.debug("updateUserPassword: userId={}", userDTO.getUserId());
        sqlSession.update("user.updateUserPassword", userDTO);
    }
}
