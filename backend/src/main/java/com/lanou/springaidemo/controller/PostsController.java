package com.lanou.springaidemo.controller;

import com.lanou.springaidemo.dto.request.PostCreateRequest;
import com.lanou.springaidemo.dto.response.ApiResponse;
import com.lanou.springaidemo.dto.response.PageResponse;
import com.lanou.springaidemo.dto.response.PostResponse;
import com.lanou.springaidemo.entity.Users;
import com.lanou.springaidemo.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lanou
 * @since 2026-06-08
 */
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostsController {
    private final PostService postService;

    @GetMapping("/public/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> getPostById(@PathVariable Long id) {
        PostResponse post = postService.getPostById(id);
        return ResponseEntity.ok(ApiResponse.success(post));
    }

    @GetMapping("/public/list")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<PostResponse> posts = postService.getPublishedPosts(page, size);
        long total = postService.countPublishedPosts();
        PageResponse<PostResponse> response = PageResponse.of(posts, total, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/public/search")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<PostResponse> posts = postService.searchPosts(keyword, page, size);
        long total = postService.countSearchPosts(keyword);
        PageResponse<PostResponse> response = PageResponse.of(posts, total, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/public/latest")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getLatestPosts() {
        List<PostResponse> posts = postService.getLatestPosts();
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @Valid @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal Users user) {
        PostResponse post = postService.createPost(user, request);
        return ResponseEntity.ok(ApiResponse.success("文章创建成功", post));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal Users user) {
        PostResponse post = postService.updatePost(id, user, request);
        return ResponseEntity.ok(ApiResponse.success("文章更新成功", post));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal Users user) {
        postService.deletePost(id, user);
        return ResponseEntity.ok(ApiResponse.success("文章删除成功", null));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getMyPosts(
            @AuthenticationPrincipal Users user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<PostResponse> posts = postService.getPostsByAuthor(user.getId(), page, size);
        long total = postService.countPostsByAuthor(user.getId());
        PageResponse<PostResponse> response = PageResponse.of(posts, total, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
