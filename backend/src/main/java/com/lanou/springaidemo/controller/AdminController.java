package com.lanou.springaidemo.controller;

import com.lanou.springaidemo.dto.response.ApiResponse;
import com.lanou.springaidemo.dto.response.PageResponse;
import com.lanou.springaidemo.dto.response.PostResponse;
import com.lanou.springaidemo.dto.response.UserResponse;
import com.lanou.springaidemo.entity.Users;
import com.lanou.springaidemo.service.PostService;
import com.lanou.springaidemo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final PostService postService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<UserResponse> users = userService.loadAllUsers(page, size);
        long total = userService.countAllUsers();
        PageResponse<UserResponse> response = PageResponse.of(users, total, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        UserResponse user = userService.updateUserByAdmin(id, request);
        return ResponseEntity.ok(ApiResponse.success("用户更新成功", user));
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("用户删除成功", null));
    }

    @GetMapping("/posts/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getPendingPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<PostResponse> posts = postService.getPendingPosts(page, size);
        long total = postService.countPendingPosts();
        PageResponse<PostResponse> response = PageResponse.of(posts, total, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/posts/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PostResponse>> approvePost(@PathVariable Long id) {
        PostResponse post = postService.approvePost(id);
        return ResponseEntity.ok(ApiResponse.success("文章审核通过", post));
    }

    @PostMapping("/posts/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PostResponse>> rejectPost(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String reason = request.getOrDefault("reason", "内容不符合规范");
        PostResponse post = postService.rejectPost(id, reason);
        return ResponseEntity.ok(ApiResponse.success("文章已拒绝", post));
    }

    @DeleteMapping("/posts/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long id) {
        Users admin = Users.builder().id(0L).role(Users.Role.ADMIN).build();
        postService.deletePost(id, admin);
        return ResponseEntity.ok(ApiResponse.success("文章删除成功", null));
    }
}
