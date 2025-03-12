package com.br.brlog.user.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.br.brlog.common.dto.ResponseDTO;
import com.br.brlog.redis.RedisService;
import com.br.brlog.user.dto.UserDTO;
import com.br.brlog.user.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping(value="/user")
public class UserController {
	private final UserService userService;
	private final RedisService redisService;

//	@SuppressWarnings({"rawtypes", "unchecked"})
//	@PostMapping("/selectUserList")
//	public ResponseEntity<List<UserDTO>> selectUserList(@RequestBody UserDTO dto) throws Exception
//	{
//		List<UserDTO> resList = userService.selectUserList(dto);
//		
//		return ResponseEntity.ok(resList);
//	}
	
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
}
