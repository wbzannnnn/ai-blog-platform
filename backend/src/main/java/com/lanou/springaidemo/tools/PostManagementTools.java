package com.lanou.springaidemo.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanou.springaidemo.entity.Posts;
import com.lanou.springaidemo.entity.Users;
import com.lanou.springaidemo.enums.ModerationStatus;
import com.lanou.springaidemo.enums.Status;
import com.lanou.springaidemo.mapper.PostsMapper;
import com.lanou.springaidemo.mapper.UsersMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文章管理工具集
 * 提供给 Spring AI 调用的文章管理工具
 * 包含：文章审核、删除、创建、状态管理等功能
 */
// 交给 Spring 容器管理
@Component
// 自动注入所有 final 成员变量
@RequiredArgsConstructor
// 开启日志
@Slf4j
public class PostManagementTools {

    // 文章 Mapper，操作文章表
    private final PostsMapper postMapper;
    // 用户 Mapper，操作用户表
    private final UsersMapper userMapper;

    /**
     * AI 工具：审核文章（通过）
     * 将文章状态改为已发布，审核状态改为通过
     */
    @Tool(name = "approve_post", description = "通过文章审核，将文章状态设置为已发布。仅管理员可用。")
    public Map<String, Object> approvePost(
            @ToolParam(description = "文章ID", required = true) Long postId,
            @ToolParam(description = "审核备注", required = false) String comment) {

        // 打印 AI 调用日志
        log.info("Tool Calling: approve_post - 文章ID: {}, 备注: {}", postId, comment);

        // 根据文章 ID 查询文章
        Posts post = postMapper.selectById(postId);

        // 如果文章不存在，返回错误信息
        if (post == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "文章不存在");
            return result;
        }

        // 设置文章状态为【已发布】
        post.setStatus(Status.PUBLISHED);
        // 设置审核状态为【通过】
        post.setModerationStatus(ModerationStatus.APPROVED);
        // 更新时间为当前时间戳
        post.setUpdatedAt(System.currentTimeMillis());
        // 保存到数据库
        postMapper.updateById(post);

        // 封装返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "文章审核通过，已发布");
        result.put("postId", post.getId());
        result.put("title", post.getTitle());
        return result;
    }

    /**
     * AI 工具：拒绝文章审核
     * 将文章设为草稿，审核状态改为拒绝
     */
    @Tool(name = "reject_post", description = "拒绝文章审核，将文章状态设置为草稿。")
    public Map<String, Object> rejectPost(
            @ToolParam(description = "文章ID", required = true) Long postId,
            @ToolParam(description = "拒绝原因", required = true) String reason) {

        log.info("Tool Calling: reject_post - 文章ID: {}, 原因: {}", postId, reason);

        // 查询文章
        Posts post = postMapper.selectById(postId);

        // 文章不存在
        if (post == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "文章不存在");
            return result;
        }

        // 文章状态改为【草稿】
        post.setStatus(Status.DRAFT);
        // 审核状态改为【拒绝】
        post.setModerationStatus(ModerationStatus.REJECTED);
        // 更新时间
        post.setUpdatedAt(System.currentTimeMillis());
        // 保存
        postMapper.updateById(post);

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "文章已拒绝: " + reason);
        result.put("postId", post.getId());
        result.put("reason", reason);
        return result;
    }

    /**
     * AI 工具：删除文章
     * 管理员专用
     */
    @Tool(name = "delete_post", description = "删除指定的文章。仅管理员可用。")
    public Map<String, Object> deletePost(
            @ToolParam(description = "文章ID", required = true) Long postId) {

        log.info("Tool Calling: delete_post - 文章ID: {}", postId);

        // 查询文章
        Posts post = postMapper.selectById(postId);

        // 文章不存在
        if (post == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "文章不存在");
            return result;
        }

        // 记录文章标题，用于返回提示
        String title = post.getTitle();
        // 根据 ID 删除文章
        postMapper.deleteById(postId);

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "文章已删除: " + title);
        result.put("postId", postId);
        return result;
    }

    /**
     * AI 工具：更新文章状态
     * 可设置为已发布 / 草稿
     */
    @Tool(name = "update_post_status", description = "更新文章状态，可以将文章设为已发布、草稿或归档")
    public Map<String, Object> updatePostStatus(
            @ToolParam(description = "文章ID", required = true) Long postId,
            @ToolParam(description = "新状态：PUBLISHED-已发布，DRAFT-草稿", required = true) String newStatus) {

        log.info("Tool Calling: update_post_status - 文章ID: {}, 新状态: {}", postId, newStatus);

        // 查询文章
        Posts post = postMapper.selectById(postId);

        // 文章不存在
        if (post == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "文章不存在");
            return result;
        }

        try {
            // 将字符串转为枚举类型
            Status status = Status.valueOf(newStatus.toUpperCase());
            // 设置新状态
            post.setStatus(status);

            // 如果是发布文章，更新时间
            if (status == Status.PUBLISHED) {
                post.setUpdatedAt(System.currentTimeMillis());
            }

            // 保存到数据库
            postMapper.updateById(post);

            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "文章状态已更新为: " + newStatus);
            result.put("postId", post.getId());
            result.put("newStatus", newStatus);
            return result;
        } catch (IllegalArgumentException e) {
            // 状态值不合法
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "无效的状态值: " + newStatus);
            return result;
        }
    }

    /**
     * AI 工具：创建新文章
     * 默认是草稿 + 待审核
     */
    @Tool(name = "create_post", description = "创建新文章")
    public Map<String, Object> createPost(
            @ToolParam(description = "文章标题", required = true) String title,
            @ToolParam(description = "文章内容", required = true) String content,
            @ToolParam(description = "作者ID", required = true) Long authorId,
            @ToolParam(description = "摘要", required = false) String summary) {

        log.info("Tool Calling: create_post - 标题: {}, 作者ID: {}", title, authorId);

        // 查询作者是否存在
        Users author = userMapper.selectById(authorId);
        if (author == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "作者不存在");
            return result;
        }

        // 构建文章对象
        Posts post = Posts.builder()
                .title(title)                                    // 标题
                .content(content)                                // 内容
                .summary(summary != null ? summary : content.substring(0, Math.min(200, content.length()))) // 摘要
                .authorId(author.getId())                       // 作者ID
                .status(Status.DRAFT)                           // 默认草稿
                .moderationStatus(ModerationStatus.PENDING)     // 默认待审核
                .likeCount(0)                                   // 初始点赞0
                .viewCount(0)                                   // 初始浏览0
                .isAiGenerated(false)                           // 非AI生成
                .createdAt(System.currentTimeMillis())          // 创建时间
                .updatedAt(System.currentTimeMillis())          // 更新时间
                .build();

        // 插入数据库
        postMapper.insert(post);

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "文章创建成功，等待审核");
        result.put("postId", post.getId());
        result.put("title", title);
        return result;
    }

    /**
     * AI 工具：获取所有待审核文章
     */
    @Tool(name = "get_pending_posts", description = "获取所有待审核的文章列表")
    public Map<String, Object> getPendingPosts(
            @ToolParam(description = "返回数量限制", required = false) Integer limit) {

        log.info("Tool Calling: get_pending_posts - 限制: {}", limit);

        // 默认最多返回 10 条
        int size = limit != null ? limit : 10;

        // 查询审核状态为 PENDING 的文章，按创建时间倒序
        List<Posts> posts = postMapper.selectList(new LambdaQueryWrapper<Posts>()
                .eq(Posts::getModerationStatus, ModerationStatus.PENDING)
                .orderByDesc(Posts::getCreatedAt));

        // 截取限制条数
        int count = Math.min(size, posts.size());

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("total", posts.size());
        result.put("returned", count);
        result.put("posts", posts.subList(0, count));
        return result;
    }
}