package com.br.brlog.user.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.brlog.common.dto.ResponseDTO;
import com.br.brlog.redis.RedisService;
import com.br.brlog.user.dto.PasswordChangeDTO;
import com.br.brlog.user.dto.UserDTO;
import com.br.brlog.user.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(value="/user")
public class UserController {
	private final UserService userService;
	private final RedisService redisService;

	///////////////////////////// 테스트 START /////////////////////////////
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	@PostMapping("/selectUserList")
	public ResponseEntity<ResponseDTO<Map<String, Object>>> selectUserList(@RequestBody UserDTO dto) throws Exception
	{
		ResponseDTO responseDTO = ResponseDTO.from(userService.selectUserList(dto));
		
		return ResponseEntity.ok(responseDTO);
	}

	@PostMapping("/insertUser")
	public ResponseEntity<UserDTO> insertUser(@RequestBody UserDTO dto) throws Exception
	{
		List<UserDTO> resList = userService.selectUserList(dto);
		UserDTO userDTO = resList.get(0);
		redisService.saveJsonAsHash("kk", userDTO);
		return ResponseEntity.ok(dto);
	}
	
	
	@PostMapping("/redisTestSaveUser")
	public ResponseEntity<UserDTO> saveJsonAsHash(@RequestBody UserDTO dto) throws Exception
	{
		redisService.saveJsonAsHash("kk", dto, 1, TimeUnit.HOURS);
		return ResponseEntity.ok(dto);
	}

	@PostMapping("/redisTestGetUser")
	public ResponseEntity<UserDTO> redisTestGetUser(@RequestBody UserDTO dto) throws Exception
	{
		UserDTO kk = redisService.getJsonFromHash("kk", UserDTO.class);
		return ResponseEntity.ok(kk);
	}
	
	@GetMapping("/brRedisTestAllData")
	public Set<String> getRedisKeys() {
	    return redisService.getAllKeys();
	}
	
	///////////////////////////// 테스트 END /////////////////////////////
	
	/**
     * 사용자 프로필 정보 조회
     */
	@SuppressWarnings("unchecked")
    @GetMapping("/{userId}/profile")
    public ResponseEntity<ResponseDTO<UserDTO>> getUserProfile(@PathVariable(name = "userId") String userId) {
        UserDTO profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(ResponseDTO.from(profile));
    }
    
    /**
     * 사용자 프로필 정보 업데이트
     */
    @SuppressWarnings("unchecked")
    @PutMapping("/{userId}/profile")
    //@PreAuthorize("#userId == authentication.principal.username")
    public ResponseEntity<ResponseDTO<UserDTO>> updateUserProfile(
    		@PathVariable(name = "userId") String userId,
            @RequestBody UserDTO userProfileDTO) {
        
        // 요청 데이터와 경로의 userId가 일치하는지 확인
        if (!userId.equals(userProfileDTO.getUserId())) {
            return ResponseEntity.badRequest().body(ResponseDTO.of(null, com.br.brlog.common.ResponseCode.ERROR_INVALID_PARAMETER));
        }
        
        UserDTO updatedProfile = userService.updateUserProfile(userProfileDTO);
        return ResponseEntity.ok(ResponseDTO.from(updatedProfile));
    }
    
    /**
     * 사용자 비밀번호 변경
     */
    @SuppressWarnings("unchecked")
	@PutMapping("/{userId}/password")
    //@PreAuthorize("#userId == authentication.principal.username")
    public ResponseEntity<ResponseDTO<Void>> updatePassword(
    		@PathVariable(name = "userId") String userId,
            @RequestBody PasswordChangeDTO passwordChangeDTO) {
        
        // 요청 데이터와 경로의 userId가 일치하는지 확인
        if (!userId.equals(passwordChangeDTO.getUserId())) {
            return ResponseEntity.badRequest().body(ResponseDTO.of(null, com.br.brlog.common.ResponseCode.ERROR_INVALID_PARAMETER));
        }
        
        boolean success = userService.updatePassword(
                userId, 
                passwordChangeDTO.getCurrentPassword(), 
                passwordChangeDTO.getNewPassword()
        );
        
        if (success) {
            return ResponseEntity.ok(ResponseDTO.from(null));
        } else {
            return ResponseEntity.badRequest().body(ResponseDTO.of(null, com.br.brlog.common.ResponseCode.ERROR_INVALID_PASSWORD));
        }
    }
}
