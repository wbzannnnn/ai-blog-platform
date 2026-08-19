package com.lanou.springaidemo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentCreateRequest {
    @NotNull(message = "文章ID不能为空")
    private Long postId;           // 文章ID
    
    @NotBlank(message = "评论内容不能为空")
    private String content;        // 评论内容
    
    private Long parentId;         // 父评论ID（回复时使用）
}