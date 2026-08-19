package com.lanou.springaidemo.service;

import com.lanou.springaidemo.dto.request.LoginRequest;
import com.lanou.springaidemo.dto.request.RegisterRequest;
import com.lanou.springaidemo.dto.response.LoginResponse;
import com.lanou.springaidemo.dto.response.UserResponse;
import com.lanou.springaidemo.entity.Users;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author lanou
 * @since 2026-06-08
 */
public interface UserService extends IService<Users>, UserDetailsService {
    UserResponse register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    UserResponse getCurrentUser(Users user);
    UserResponse getUserById(Long id);
    UserResponse updateUser(Long id, RegisterRequest request);
    void deleteUser(Long id);
    List<UserResponse> loadAllUsers(int page, int size);
    long countAllUsers();
    UserResponse updateUserByAdmin(Long id, Map<String, Object> request);
}
