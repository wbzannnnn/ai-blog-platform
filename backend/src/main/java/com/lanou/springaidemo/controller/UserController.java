package com.lanou.springaidemo.controller;

import com.lanou.springaidemo.dto.response.ApiResponse;
import com.lanou.springaidemo.dto.response.UserResponse;
import com.lanou.springaidemo.entity.Users;
import com.lanou.springaidemo.exception.BusinessException;
import com.lanou.springaidemo.mapper.UsersMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UsersMapper usersMapper;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private Path avatarUploadPath;

    @PostConstruct
    public void init() {
        // 基于项目工作目录，确保使用绝对路径
        Path base = Paths.get(System.getProperty("user.dir"));
        avatarUploadPath = base.resolve(uploadDir).resolve("avatars").toAbsolutePath();
        try {
            Files.createDirectories(avatarUploadPath);
            log.info("头像上传目录: {}", avatarUploadPath);
        } catch (IOException e) {
            log.error("无法创建上传目录", e);
        }
    }

    /**
     * 获取当前用户个人信息
     */
    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal Users user) {
        // 重新从数据库查询以获取最新数据
        Users latest = usersMapper.selectById(user.getId());
        return ResponseEntity.ok(ApiResponse.success(UserResponse.fromEntity(latest)));
    }

    /**
     * 更新个人信息（昵称、邮箱）
     */
    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal Users user,
            @RequestBody Map<String, String> body) {
        Users entity = usersMapper.selectById(user.getId());
        if (entity == null) {
            throw new BusinessException(404, "用户不存在");
        }

        String nickname = body.get("nickname");
        String email = body.get("email");

        if (nickname != null && !nickname.isBlank()) {
            entity.setNickname(nickname.trim());
        }
        if (email != null && !email.isBlank()) {
            // 简单校验邮箱格式
            if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                throw new BusinessException(400, "邮箱格式不正确");
            }
            // 检查邮箱是否被其他人占用
            Users exist = usersMapper.findByEmail(email.trim()).orElse(null);
            if (exist != null && !exist.getId().equals(user.getId())) {
                throw new BusinessException(400, "该邮箱已被其他用户使用");
            }
            entity.setEmail(email.trim());
        }

        entity.setUpdatedAt(System.currentTimeMillis());
        usersMapper.updateById(entity);
        log.info("用户信息更新成功: {}", entity.getUsername());
        return ResponseEntity.ok(ApiResponse.success("个人信息更新成功", UserResponse.fromEntity(entity)));
    }

    /**
     * 上传头像
     */
    @PostMapping("/avatar")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(
            @AuthenticationPrincipal Users user,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的文件");
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(400, "只能上传图片文件");
        }

        // 校验文件大小（最大2MB）
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException(400, "图片大小不能超过2MB");
        }

        try {
            // 生成唯一文件名
            String originalName = file.getOriginalFilename();
            String ext = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf("."))
                    : ".png";
            String fileName = "avatar_" + user.getId() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path filePath = avatarUploadPath.resolve(fileName);

            // 保存文件
            file.transferTo(filePath.toFile());

            // 更新用户头像路径
            String avatarUrl = "/uploads/avatars/" + fileName;
            Users entity = usersMapper.selectById(user.getId());
            entity.setAvatar(avatarUrl);
            entity.setUpdatedAt(System.currentTimeMillis());
            usersMapper.updateById(entity);

            log.info("头像上传成功: {}, 路径: {}", user.getUsername(), avatarUrl);
            return ResponseEntity.ok(ApiResponse.success("头像上传成功", UserResponse.fromEntity(entity)));
        } catch (IOException e) {
            log.error("头像上传失败", e);
            throw new BusinessException(500, "头像上传失败，请稍后重试");
        }
    }
}
