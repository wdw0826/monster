package com.example.monsterhunter.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全域例外處理：Service 丟出的例外會被這裡攔截，統一轉成 {"error": "訊息"} 的 JSON
 * 加上對應的 HTTP 狀態碼，Controller 完全不用自己寫 try-catch。
 * 之後新增例外類型，就是在這裡多加一個 @ExceptionHandler 方法。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    /** @Valid 掛的欄位驗證失敗時（例如 name 傳空字串）會丟這個，統一轉成跟其他例外一樣的 {"error": ...} 格式，外加逐欄位的錯誤說明。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "欄位驗證失敗", "fields", fieldErrors));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<Map<String, String>> handleTokenExpired(TokenExpiredException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }

    /**
     * 樂觀鎖衝突（Quest / Player 上的 @Version）：兩個請求同時改同一筆資料，後寫回的那個會進到這裡。
     * 典型情境是兩個玩家同時接同一個任務、或同一個玩家連點購買。回 409 讓呼叫端知道要重新整理再試。
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.info("樂觀鎖衝突：{}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "這筆資料剛剛被其他請求修改了，請重新整理後再試一次"));
    }

    /**
     * 資料庫 unique / FK 約束擋下來的請求，例如：同時用同一個帳號名註冊、
     * 或兩個請求同時讓同一個獵人接兩個任務（V3 的 uk_quests_active_player）。
     * 不把資料庫錯誤原文回給呼叫端（會洩漏表名、欄位名），只記在 log。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.info("資料庫約束擋下請求：{}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "操作與目前資料衝突（可能是重複資料或同時送出的請求），請重新整理後再試"));
    }

    /**
     * 請求格式錯誤：JSON 壞掉、enum 值不存在（例如 action 傳 "DANCE"）、
     * 路徑或參數型別不對（例如 /api/quests/abc、?type=HUGE）。這些是呼叫端的錯，回 400。
     * 要放在下面的 Exception 兜底之前定義清楚，不然會被當成 500。
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, TypeMismatchException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "請求格式錯誤，請確認欄位名稱、型別與可用的值"));
    }

    /**
     * 最後一道防線：沒有被上面任何一個 handler 接住的例外。
     * 以前會變成 Spring 預設的 Whitelabel 500 頁面（HTML），跟其他 API 的 {"error": ...} 格式不一致。
     *
     * - Spring MVC 自己的例外（404 找不到路徑、405 方法不對、400 參數格式錯…）都實作 ErrorResponse，
     *   沿用它們原本的狀態碼，不要全部變成 500。
     * - Spring Security 的例外往外丟，交給 SecurityConfig 的 401/403 處理。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception ex) throws Exception {
        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            throw ex;
        }
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            String detail = errorResponse.getBody().getDetail();
            return ResponseEntity.status(status)
                    .body(Map.of("error", detail != null ? detail : "請求無法處理"));
        }
        log.error("未預期的錯誤", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "伺服器發生未預期的錯誤，請稍後再試"));
    }
}
