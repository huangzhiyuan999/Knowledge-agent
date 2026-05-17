package com.tiantian.yuaiagent.aspect;

import com.tiantian.yuaiagent.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * AOP 切面：拦截 @RequireAuth 注解的方法，校验 JWT token
 * 从请求头 Authorization: Bearer xxx 中提取 token
 */
@Aspect
@Component
public class AuthAspect {

    private final JwtUtil jwtUtil;

    public AuthAspect(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Around("@annotation(com.tiantian.yuaiagent.annotation.RequireAuth)")
    public Object checkAuth(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取当前请求
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return ResponseEntity.status(401).body(Map.of("error", "无请求上下文"));
        }
        HttpServletRequest request = attrs.getRequest();

        // 提取 Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "缺少 token"));
        }

        String token = authHeader.substring(7);
        String userId = jwtUtil.validateAccessToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "token 无效或已过期"));
        }

        // 将 userId 存入 request 属性，Controller 可获取
        request.setAttribute("userId", userId);
        return joinPoint.proceed();
    }
}
