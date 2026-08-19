package com.lanou.springaidemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanou.springaidemo.dto.request.LoginRequest;
import com.lanou.springaidemo.dto.request.RegisterRequest;
import com.lanou.springaidemo.dto.response.LoginResponse;
import com.lanou.springaidemo.dto.response.UserResponse;
import com.lanou.springaidemo.entity.Users;
import com.lanou.springaidemo.exception.BusinessException;
import com.lanou.springaidemo.mapper.UsersMapper;
import com.lanou.springaidemo.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lanou.springaidemo.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author lanou
 * @since 2026-06-08
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl extends ServiceImpl<UsersMapper, Users> implements UserService {
    private final UsersMapper usersMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return usersMapper.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
    }

    @Transactional
    @Override
    public UserResponse register(RegisterRequest request) {
        if (usersMapper.existsByUsername(request.getUsername())) {
            throw new BusinessException(400, "用户名已存在");
        }
        if (usersMapper.existsByEmail(request.getEmail())) {
            throw new BusinessException(400, "邮箱已被注册");
        }

        Users user = Users.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .nickname(request.getNickname())
                .role(Users.Role.USER)
                .status(true)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        usersMapper.insert(user);
        log.info("用户注册成功: {}", user.getUsername());
        return UserResponse.fromEntity(user);
    }

    /**
     * 用户登录核心逻辑
     * 手动验证密码，避免依赖 AuthenticationManager（解决循环依赖和Provider未配置问题）
     * @param request 登录请求
     * @return 登录响应（包含token和用户信息）
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 根据用户名查询用户
        Users user = usersMapper.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("登录失败：用户不存在 - {}", request.getUsername());
                    return new BusinessException(401, "用户名或密码错误");
                });

        // 2. 检查账号是否启用
        if (Boolean.FALSE.equals(user.getStatus())) {
            log.warn("登录失败：账号已禁用 - {}", request.getUsername());
            throw new BusinessException(403, "账号已被禁用");
        }

        // 3. 验证密码（BCrypt）
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("登录失败：密码错误 - {}", request.getUsername());
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 4. 生成JWT令牌
        String token = jwtUtil.generateToken(user.getUsername());

        log.info("用户登录成功: {}", user.getUsername());

        // 5. 构建响应对象
        return LoginResponse.builder()
                .token(token)
                .user(UserResponse.fromEntity(user))
                .build();
    }


    @Override
    public UserResponse getCurrentUser(Users user) {
        return UserResponse.fromEntity(user);
    }

    @Override
    public UserResponse getUserById(Long id) {
        Users user = usersMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        return UserResponse.fromEntity(user);
    }

    @Transactional
    @Override
    public UserResponse updateUser(Long id, RegisterRequest request) {
        Users user = usersMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (!user.getUsername().equals(request.getUsername()) && usersMapper.existsByUsername(request.getUsername())) {
            throw new BusinessException(400, "用户名已存在");
        }
        if (!user.getEmail().equals(request.getEmail()) && usersMapper.existsByEmail(request.getEmail())) {
            throw new BusinessException(400, "邮箱已被注册");
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setNickname(request.getNickname());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setUpdatedAt(System.currentTimeMillis());

        usersMapper.updateById(user);
        return UserResponse.fromEntity(user);
    }

    @Transactional
    @Override
    public void deleteUser(Long id) {
        if (usersMapper.selectById(id) == null) {
            throw new BusinessException(404, "用户不存在");
        }
        usersMapper.deleteById(id);
        log.info("用户删除成功: {}", id);
    }

    @Override
    public List<UserResponse> loadAllUsers(int page, int size) {
        List<Users> users = usersMapper.selectList(new LambdaQueryWrapper<Users>().orderByDesc(Users::getCreatedAt));
        int start = page * size;
        if (start >= users.size()) {
            return new ArrayList<>();
        }
        int end = Math.min(start + size, users.size());
        return users.subList(start, end).stream().map(UserResponse::fromEntity).toList();
    }

    @Override
    public long countAllUsers() {
        return usersMapper.selectCount(null);
    }

    @Transactional
    @Override
    public UserResponse updateUserByAdmin(Long id, Map<String, Object> request) {
        Users user = usersMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (request.containsKey("username")) {
            String newUsername = (String) request.get("username");
            if (!user.getUsername().equals(newUsername) && usersMapper.existsByUsername(newUsername)) {
                throw new BusinessException(400, "用户名已存在");
            }
            user.setUsername(newUsername);
        }

        if (request.containsKey("email")) {
            String newEmail = (String) request.get("email");
            if (!user.getEmail().equals(newEmail) && usersMapper.existsByEmail(newEmail)) {
                throw new BusinessException(400, "邮箱已被注册");
            }
            user.setEmail(newEmail);
        }

        if (request.containsKey("nickname")) {
            user.setNickname((String) request.get("nickname"));
        }

        if (request.containsKey("role")) {
            user.setRole(Users.Role.valueOf((String) request.get("role")));
        }

        if (request.containsKey("enabled")) {
            user.setStatus((Boolean) request.get("enabled"));
        }

        user.setUpdatedAt(System.currentTimeMillis());
        usersMapper.updateById(user);
        return UserResponse.fromEntity(user);
    }

}
