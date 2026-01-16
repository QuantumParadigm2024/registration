package com.planotech.plano.exception;

public class CustomBadRequestException extends RuntimeException{
    String message;
    public CustomBadRequestException(String message){
        this.message=message;
    }
}
