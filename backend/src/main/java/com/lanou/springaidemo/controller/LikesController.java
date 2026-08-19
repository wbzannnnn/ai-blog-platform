package com.lanou.springaidemo.controller;

import com.lanou.springaidemo.dto.response.ApiResponse;
import com.lanou.springaidemo.entity.Users;
import com.lanou.springaidemo.service.LikesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lanou
 * @since 2026-06-08
 */
@RestController
@RequestMapping("/likes")
@RequiredArgsConstructor
public class LikesController {
    private final LikesService likeService;

    @PostMapping("/post/{postId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> likePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Users user) {
        likeService.likePost(user, postId);
        return ResponseEntity.ok(ApiResponse.success("点赞成功", null));
    }

    @DeleteMapping("/post/{postId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unlikePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Users user) {
        likeService.unlikePost(user, postId);
        return ResponseEntity.ok(ApiResponse.success("取消点赞", null));
    }

    @PostMapping("/comment/{commentId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> likeComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal Users user) {
        likeService.likeComment(user, commentId);
        return ResponseEntity.ok(ApiResponse.success("点赞成功", null));
    }

    @DeleteMapping("/comment/{commentId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> unlikeComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal Users user) {
        likeService.unlikeComment(user, commentId);
        return ResponseEntity.ok(ApiResponse.success("取消点赞", null));
    }

    /**
     * 批量查询点赞状态
     * GET /likes/check?postId=1&commentIds=2,3,4
     * 返回: { "postLiked": true, "likedCommentIds": [2, 3] }
     */
    @GetMapping("/check")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkLikeStatus(
            @RequestParam(required = false) Long postId,
            @RequestParam(required = false) List<Long> commentIds,
            @AuthenticationPrincipal Users user) {
        Map<String, Object> result = new HashMap<>();
        if (postId != null) {
            result.put("postLiked", likeService.isPostLiked(user, postId));
        }
        if (commentIds != null && !commentIds.isEmpty()) {
            List<Long> likedIds = commentIds.stream()
                    .filter(id -> likeService.isCommentLiked(user, id))
                    .toList();
            result.put("likedCommentIds", likedIds);
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
