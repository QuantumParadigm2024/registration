package com.planotech.plano.exception;

import org.springframework.http.HttpStatus;

public class CustomJwtException extends  RuntimeException{

    String message;
    public CustomJwtException(String message){
        this.message=message;
    }
}
