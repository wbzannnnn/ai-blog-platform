package com.lanou.springaidemo.controller;

import com.lanou.springaidemo.dto.request.CommentCreateRequest;
import com.lanou.springaidemo.dto.response.ApiResponse;
import com.lanou.springaidemo.dto.response.CommentResponse;
import com.lanou.springaidemo.entity.Users;
import com.lanou.springaidemo.service.CommentsService;
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
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentsController {

    private final CommentsService commentService;

    @GetMapping("/post/{postId}")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getCommentsByPost(@PathVariable Long postId) {
        List<CommentResponse> comments = commentService.getCommentsByPost(postId);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @Valid @RequestBody CommentCreateRequest request,
            @AuthenticationPrincipal Users user) {
        CommentResponse comment = commentService.createComment(user, request);
        return ResponseEntity.ok(ApiResponse.success("评论成功", comment));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal Users user) {
        commentService.deleteComment(id, user);
        return ResponseEntity.ok(ApiResponse.success("评论删除成功", null));
    }
}
