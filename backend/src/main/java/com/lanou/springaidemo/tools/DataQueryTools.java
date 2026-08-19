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
 * 数据查询工具集
 * 给 Spring AI 大模型使用：查询博客系统各类数据
 */
// 交给Spring容器管理
@Component
// 自动注入所有final字段
@RequiredArgsConstructor
// 日志注解
@Slf4j
public class DataQueryTools {

    // 文章Mapper
    private final PostsMapper postMapper;
    // 用户Mapper
    private final UsersMapper userMapper;

    /**
     * AI工具：获取博客系统整体统计数据
     */
    @Tool(name = "get_statistics", description = "获取博客系统的统计数据，包括用户数、文章数等")
    public Map<String, Object> getStatistics() {
        // 打印调用日志
        log.info("Tool Calling: get_statistics - 获取系统统计数据");

        // 封装统计结果
        Map<String, Object> stats = new HashMap<>();
        // 总用户数
        stats.put("totalUsers", userMapper.selectCount(null));
        // 总文章数
        stats.put("totalPosts", postMapper.selectCount(null));
        // 已发布文章数
        stats.put("publishedPosts", postMapper.selectCount(new LambdaQueryWrapper<Posts>().eq(Posts::getStatus, Status.PUBLISHED)));
        // 待审核文章数
        stats.put("pendingPosts", postMapper.selectCount(new LambdaQueryWrapper<Posts>().eq(Posts::getModerationStatus, ModerationStatus.PENDING)));
        // 评论数（默认0）
        stats.put("totalComments", 0);

        return stats;
    }

    /**
     * AI工具：根据文章ID获取文章详情
     */
    @Tool(name = "get_post_detail", description = "根据文章ID获取文章详情")
    public Posts getPostDetail(
            @ToolParam(description = "文章ID", required = true) Long postId) {

        log.info("Tool Calling: get_post_detail - 文章ID: {}", postId);

        // 根据ID查询文章并返回
        return postMapper.selectById(postId);
    }

    /**
     * AI工具：获取文章列表，支持按状态筛选
     */
    @Tool(name = "get_posts_list", description = "获取文章列表，支持按状态筛选")
    public List<Posts> getPostsList(
            @ToolParam(description = "文章状态：PUBLISHED-已发布，DRAFT-草稿", required = false) String status,
            @ToolParam(description = "返回数量限制", required = false) Integer limit) {

        log.info("Tool Calling: get_posts_list - 状态: {}, 限制: {}", status, limit);

        List<Posts> posts;
        // 限制条数，默认10
        int size = limit != null ? limit : 10;

        // 如果传入了状态，按状态查询
        if (status != null && !status.isEmpty()) {
            Status postStatus = Status.valueOf(status);
            posts = postMapper.selectList(new LambdaQueryWrapper<Posts>()
                    .eq(Posts::getStatus, postStatus)
                    .orderByDesc(Posts::getCreatedAt));
        } else {
            // 没传状态，默认只查已发布文章
            posts = postMapper.selectList(new LambdaQueryWrapper<Posts>()
                    .eq(Posts::getStatus, Status.PUBLISHED)
                    .orderByDesc(Posts::getCreatedAt));
        }

        // 截取指定条数返回
        return size < posts.size() ? posts.subList(0, size) : posts;
    }

    /**
     * AI工具：根据用户ID获取用户详情
     */
    @Tool(name = "get_user_detail", description = "根据用户ID获取用户详细信息")
    public Users getUserDetail(
            @ToolParam(description = "用户ID", required = true) Long userId) {

        log.info("Tool Calling: get_user_detail - 用户ID: {}", userId);

        // 根据ID查询用户
        return userMapper.selectById(userId);
    }

    /**
     * AI工具：获取用户列表
     */
    @Tool(name = "get_users_list", description = "获取用户列表")
    public List<Users> getUsersList(
            @ToolParam(description = "返回数量限制", required = false) Integer limit) {

        log.info("Tool Calling: get_users_list - 限制: {}", limit);

        // 默认10条
        int size = limit != null ? limit : 10;
        // 查询所有用户
        List<Users> users = userMapper.selectList(null);

        // 截取条数
        return size < users.size() ? users.subList(0, size) : users;
    }

    /**
     * AI工具：获取热门文章（按浏览量排序）
     */
    @Tool(name = "get_hot_posts", description = "获取浏览量最高的文章列表")
    public List<Posts> getHotPosts(
            @ToolParam(description = "返回数量限制", required = false) Integer limit) {

        log.info("Tool Calling: get_hot_posts - 限制: {}", limit);

        int size = limit != null ? limit : 10;

        // 查询已发布文章，按浏览量倒序，限制条数
        return postMapper.selectList(new LambdaQueryWrapper<Posts>()
                .eq(Posts::getStatus, Status.PUBLISHED)
                .orderByDesc(Posts::getViewCount)
                .last("LIMIT " + size));
    }

    /**
     * AI工具：获取最近发布的文章
     */
    @Tool(name = "get_recent_posts", description = "获取最近发布的文章列表")
    public List<Posts> getRecentPosts(
            @ToolParam(description = "返回数量限制", required = false) Integer limit) {

        log.info("Tool Calling: get_recent_posts - 限制: {}", limit);

        int size = limit != null ? limit : 10;

        // 按创建时间倒序
        return postMapper.selectList(new LambdaQueryWrapper<Posts>()
                .orderByDesc(Posts::getCreatedAt)
                .last("LIMIT " + size));
    }

    /**
     * AI工具：获取文章数量统计（可按状态）
     */
    @Tool(name = "get_post_count", description = "获取文章数量统计信息")
    public Map<String, Object> getPostCount(
            @ToolParam(description = "文章状态，不传则统计所有", required = false) String status) {

        log.info("Tool Calling: get_post_count - 状态: {}", status);

        Map<String, Object> countMap = new HashMap<>();

        // 如果指定了状态，只统计该状态
        if (status != null && !status.isEmpty()) {
            Status postStatus = Status.valueOf(status);
            countMap.put(status.toLowerCase(), postMapper.selectCount(new LambdaQueryWrapper<Posts>().eq(Posts::getStatus, postStatus)));
        } else {
            // 未指定状态，统计全部分类
            countMap.put("total", postMapper.selectCount(null));
            countMap.put("published", postMapper.selectCount(new LambdaQueryWrapper<Posts>().eq(Posts::getStatus, Status.PUBLISHED)));
            countMap.put("pending", postMapper.selectCount(new LambdaQueryWrapper<Posts>().eq(Posts::getModerationStatus, ModerationStatus.PENDING)));
            countMap.put("draft", postMapper.selectCount(new LambdaQueryWrapper<Posts>().eq(Posts::getStatus, Status.DRAFT)));
        }

        return countMap;
    }
}