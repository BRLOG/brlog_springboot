package com.br.brlog.redis;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

	private final RedisTemplate<String, Object> redisTemplate;
	private final ObjectMapper objectMapper;
	@Autowired
	public RedisService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
	    this.redisTemplate = redisTemplate;
	    this.objectMapper = objectMapper;
	}
	
	// 객체로 통으로 저장
	public void saveJsonAsHash(String key, Object jsonObject) {
	    Map<String, Object> map = objectMapper.convertValue(jsonObject, Map.class);
	    redisTemplate.opsForHash().putAll(key, map);
	}
	
	// 객체 통으로 저장 만료시간 설정버전
	public void saveJsonAsHash(String key, Object jsonObject, long timeout, TimeUnit timeUnit) {
	    Map<String, Object> map = objectMapper.convertValue(jsonObject, Map.class);
	    redisTemplate.opsForHash().putAll(key, map);
	    redisTemplate.expire(key, timeout, timeUnit);
	}
	
	// 객체 변환
	public <T> T getJsonFromHash(String key, Class<T> clazz) {
	    Map<Object, Object> map = redisTemplate.opsForHash().entries(key);
	    return objectMapper.convertValue(map, clazz);
	}
	
	// 특정 필드 수정
	public void updateJsonField(String key, String field, Object value) {
	    redisTemplate.opsForHash().put(key, field, value);
	}
	
	// 특정 필드 꺼내기
	public Object getJsonField(String key, String field) {
	    return redisTemplate.opsForHash().get(key, field);
	}
	
	// 데이터 저장
	public void saveData(String key, Object value) {
	    redisTemplate.opsForValue().set(key, value);
	}
	
	// 데이터 저장 (만료 시간 설정)
	public void saveDataWithExpiration(String key, Object value, long timeout, TimeUnit timeUnit) {
	    redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
	}
	
	// 데이터 조회
	public Object getData(String key) {
	    return redisTemplate.opsForValue().get(key);
	}
	
	// String 데이터 조회
	public String getStringData(String key) {
	    Object value = redisTemplate.opsForValue().get(key);
	    return value != null ? value.toString() : null;
	}
	
	public <T> T getData(String key, Class<T> clazz) {
	    Object value = redisTemplate.opsForValue().get(key);
	    if (value == null) {
	        return null;
	    }
	    if (clazz.isInstance(value)) {
	        return clazz.cast(value);
	    }
	    if (clazz == String.class) {
	        return clazz.cast(value.toString());
	    }
	    throw new ClassCastException("Cannot cast " + value.getClass() + " to " + clazz);
	}
	
	// 데이터 삭제
	public void deleteData(String key) {
	    redisTemplate.delete(key);
	}
	
	// 키 존재 여부 확인
	public boolean hasKey(String key) {
	    return redisTemplate.hasKey(key);
	}
	
	// 리스트에 데이터 추가
	public void addToList(String key, Object value) {
	    redisTemplate.opsForList().rightPush(key, value);
	}
	
	// 리스트에서 데이터 가져오기
	public Object getFromList(String key, long index) {
	    return redisTemplate.opsForList().index(key, index);
	}
	
	// 해시에 데이터 저장
	public void saveHash(String key, String hashKey, Object value) {
	    redisTemplate.opsForHash().put(key, hashKey, value);
	}
	
	// 해시에서 데이터 조회
	public Object getHash(String key, String hashKey) {
	    return redisTemplate.opsForHash().get(key, hashKey);
	}
	
	public Set<String> getAllKeys() {
	    return redisTemplate.keys("*");
	}
}