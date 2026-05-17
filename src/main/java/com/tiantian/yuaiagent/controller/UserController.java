package com.tiantian.yuaiagent.controller;

import com.tiantian.yuaiagent.annotation.RequireAuth;
import com.tiantian.yuaiagent.dao.UserDao;
import com.tiantian.yuaiagent.model.User;
import com.tiantian.yuaiagent.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户认证接口
 * 提供登录、刷新 token、登出功能
 */
@RestController
@RequestMapping("/user")
public class UserController {

    private final JwtUtil jwtUtil;
    private final UserDao userDao;

    public UserController(JwtUtil jwtUtil, UserDao userDao) {
        this.jwtUtil = jwtUtil;
        this.userDao = userDao;
    }

    /** 登录：校验账号密码，返回双令牌 */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        User user = userDao.findByName(username);
        if (user == null || !user.getPassword().equals(password)) {
            return ResponseEntity.status(401).body(Map.of("error", "账号或密码错误"));
        }

        String userId = String.valueOf(user.getId());
        String accessToken = jwtUtil.generateAccessToken(userId);
        String refreshToken = jwtUtil.generateRefreshToken(userId);

        return ResponseEntity.ok(Map.of(
                "access_token", accessToken,
                "refresh_token", refreshToken,
                "token_type", "Bearer"
        ));
    }

    /** 刷新 access_token */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refresh_token");
        if (refreshToken == null) {
            return ResponseEntity.status(400).body(Map.of("error", "缺少 refresh_token"));
        }

        String newAccessToken = jwtUtil.refreshAccessToken(refreshToken);
        if (newAccessToken == null) {
            return ResponseEntity.status(401).body(Map.of("error", "refresh_token 无效或已过期"));
        }

        return ResponseEntity.ok(Map.of("access_token", newAccessToken));
    }

    /** 登出（需登录） */
    @PostMapping("/logout")
    @RequireAuth
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        jwtUtil.logout(userId);
        return ResponseEntity.ok(Map.of("message", "已退出"));
    }

    /** 获取当前用户信息（需登录） */
    @GetMapping("/me")
    @RequireAuth
    public ResponseEntity<Map<String, Object>> me(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return ResponseEntity.ok(Map.of("userId", userId, "username", "admin"));
    }
}
