package com.br.brlog.common.dto;

import com.br.brlog.common.ResponseCode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseDTO<T> {
    private boolean result;
    private String code;
    private String message;
    private String detail;
    private T data;
    

    public ResponseDTO(T data) {
        this(data, ResponseCode.SUCCESS_CODE);
    }

    public ResponseDTO(T data, ResponseCode responseCode) {
        this.result = responseCode.isResult();
        this.data = data;
        this.code = responseCode.getCode();
        this.message = responseCode.getMessage();
    }
    
    public static <T> ResponseDTO from(T data) {
        return new ResponseDTO(data);
    }

    public static <T> ResponseDTO of(T data, ResponseCode responseCode) {
        return new ResponseDTO(data, responseCode);
    }

}