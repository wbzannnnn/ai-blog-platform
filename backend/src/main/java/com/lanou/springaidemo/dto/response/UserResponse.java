package com.lanou.springaidemo.dto.response;

import com.lanou.springaidemo.entity.Users;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatar;
    private String role;
    private Long createdAt;

    // 从实体转换
    public static UserResponse fromEntity(Users user) {
        UserResponse response = new UserResponse();
        response.id = user.getId();
        response.username = user.getUsername();
        response.email = user.getEmail();
        response.nickname = user.getNickname();
        response.avatar = user.getAvatar();
        response.role = user.getRole().name();
        response.createdAt = user.getCreatedAt();
        return response;
    }
}