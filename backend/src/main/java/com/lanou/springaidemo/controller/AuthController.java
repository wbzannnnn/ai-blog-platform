package com.lanou.springaidemo.controller;

import com.lanou.springaidemo.dto.request.LoginRequest;
import com.lanou.springaidemo.dto.request.RegisterRequest;
import com.lanou.springaidemo.dto.response.ApiResponse;
import com.lanou.springaidemo.dto.response.LoginResponse;
import com.lanou.springaidemo.dto.response.UserResponse;
import com.lanou.springaidemo.entity.Users;
import com.lanou.springaidemo.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户注册接口
     * POST /api/auth/register
     * @param request 注册请求（@Valid触发参数校验）
     * @return 注册成功的用户信息
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        UserResponse user = userService.register(request);
        return ResponseEntity.ok(ApiResponse.success("注册成功", user));
    }

    /**
     * 用户登录接口
     * POST /api/auth/login
     * @param request 登录请求
     * @return 登录成功的token和用户信息
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse loginResponse = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success("登录成功", loginResponse));
    }

    /**
     * 获取当前登录用户信息
     * GET /api/auth/me
     * @param user 当前认证用户（由@AuthenticationPrincipal注入）
     * @return 当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal Users user) {
        UserResponse userResponse = userService.getCurrentUser(user);
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }
}