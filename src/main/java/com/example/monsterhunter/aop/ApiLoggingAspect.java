package com.example.monsterhunter.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 攔截所有 Controller 方法，統一記錄「打了哪支 API、花多久、成功還是丟例外」，
 * 不用在每支方法裡自己寫 log。
 * 刻意不印方法參數本身：LoginRequest/RegisterRequest 這類請求 DTO 裡帶明文密碼，
 * 把參數整包印出來等於把密碼寫進 log 檔，是常見的資安地雷，所以這裡只記錄
 * 「打了哪個網址、哪個方法、結果如何」，不記錄請求內容。
 */
@Slf4j
@Aspect
@Component
public class ApiLoggingAspect {

    @Around("execution(public * com.example.monsterhunter.controller..*.*(..))")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String signature = joinPoint.getSignature().toShortString();
        String requestInfo = currentRequestInfo();
        long startTime = System.currentTimeMillis();

        log.info("[API] --> {} {}", requestInfo, signature);

        try {
            Object result = joinPoint.proceed();
            log.info("[API] <-- {} {} 成功，耗時 {}ms", requestInfo, signature, System.currentTimeMillis() - startTime);
            return result;
        } catch (Throwable ex) {
            log.warn("[API] <-- {} {} 拋出 {}：{}，耗時 {}ms",
                    requestInfo, signature, ex.getClass().getSimpleName(), ex.getMessage(),
                    System.currentTimeMillis() - startTime);
            throw ex;
        }
    }

    private String currentRequestInfo() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            return request.getMethod() + " " + request.getRequestURI();
        }
        return "N/A";
    }
}
