package com.example.monsterhunter.exception;

/** 查詢的資源（獵人、任務…）不存在時丟出，GlobalExceptionHandler 會轉成 404。 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
