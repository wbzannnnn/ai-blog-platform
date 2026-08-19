package com.lanou.springaidemo.dto.response;

import com.lanou.springaidemo.entity.Comments;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 评论响应对象
 * 用于给前端返回评论详情数据（不暴露数据库实体，更安全）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    // 评论ID（唯一标识）
    private Long id;

    // 评论内容
    private String content;

    // 评论作者信息（关联用户返回对象）
    private UserResponse author;

    // 父评论ID（0 = 一级评论，其他 = 回复某条评论）
    private Long parentId;

    // 回复列表（子评论，递归结构）
    private List<CommentResponse> replies;

    // 点赞数量
    private Integer likeCount;

    // 创建时间（时间戳）
    private Long createdAt;

    /**
     * 静态方法：把数据库实体 Comments 转换成 前端响应对象 CommentResponse
     * @param comment 数据库查询到的评论实体
     * @param author  评论的作者信息（已封装好的UserResponse）
     * @return 可直接返回给前端的评论数据
     */
    public static CommentResponse fromEntity(Comments comment, UserResponse author) {
        // 使用 Builder 模式构建响应对象
        return CommentResponse.builder()
                // 设置评论ID
                .id(comment.getId())
                // 设置评论内容
                .content(comment.getContent())
                // 设置作者信息
                .author(author)
                // 设置父评论ID
                .parentId(comment.getParentId())
                // 初始化空的回复列表（后续再填充子评论）
                .replies(List.of())
                // 设置点赞数
                .likeCount(comment.getLikeCount())
                // 设置创建时间
                .createdAt(comment.getCreatedAt())
                // 构建完成，返回对象
                .build();
    }
}