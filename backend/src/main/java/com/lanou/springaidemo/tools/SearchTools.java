package com.lanou.springaidemo.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanou.springaidemo.entity.Posts;
import com.lanou.springaidemo.entity.Tags;
import com.lanou.springaidemo.entity.Users;
import com.lanou.springaidemo.enums.Status;
import com.lanou.springaidemo.mapper.PostsMapper;
import com.lanou.springaidemo.mapper.TagsMapper;
import com.lanou.springaidemo.mapper.UsersMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能搜索工具集
 * 提供给 Spring AI 大模型调用的工具类
 * 包含：文章搜索、标签搜索、相关推荐等功能
 */
// 交给Spring管理，成为Bean
@Component
// 自动注入所有final成员变量
@RequiredArgsConstructor
// 开启日志
@Slf4j
public class SearchTools {

    // 文章Mapper
    private final PostsMapper postMapper;
    // 标签Mapper
    private final TagsMapper tagMapper;
    // 用户Mapper
    private final UsersMapper userMapper;

    /**
     * AI工具：根据关键词搜索文章
     * 搜索范围：文章标题 + 内容
     */
    @Tool(name = "search_posts", description = "根据关键词搜索文章，搜索范围包括文章标题和内容")
    public Map<String, Object> searchPosts(
            @ToolParam(description = "搜索关键词", required = true) String keyword,
            @ToolParam(description = "返回结果数量限制", required = false) Integer limit) {

        // 打印AI调用日志
        log.info("Tool Calling: search_posts - 关键词: {}, 限制: {}", keyword, limit);

        // 限制条数，不传默认10条
        int size = limit != null ? limit : 10;

        // 调用Mapper，查询已发布且包含关键词的文章
        List<Posts> posts = postMapper.searchByKeyword(Status.PUBLISHED, keyword);

        // 如果结果超过限制，只取前N条
        if (posts.size() > size) {
            posts = posts.subList(0, size);
        }

        // 封装返回结果（AI需要结构化数据）
        Map<String, Object> result = new HashMap<>();
        result.put("keyword", keyword);        // 搜索关键词
        result.put("total", posts.size());     // 总条数
        result.put("posts", posts);             // 文章列表
        return result;
    }

    /**
     * AI工具：根据标签名称搜索文章
     */
    @Tool(name = "search_posts_by_tag", description = "根据标签名称搜索所有相关的文章")
    public Map<String, Object> searchPostsByTag(
            @ToolParam(description = "标签名称", required = true) String tagName,
            @ToolParam(description = "返回结果数量限制", required = false) Integer limit) {

        log.info("Tool Calling: search_posts_by_tag - 标签: {}, 限制: {}", tagName, limit);

        int size = limit != null ? limit : 10;

        // 根据标签名查询已发布文章
        List<Posts> posts = postMapper.findByTagName(Status.PUBLISHED, tagName);

        if (posts.size() > size) {
            posts = posts.subList(0, size);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("tagName", tagName);
        result.put("total", posts.size());
        result.put("posts", posts);
        return result;
    }

    /**
     * AI工具：根据作者用户名搜索该作者的所有文章
     */
    @Tool(name = "search_posts_by_author", description = "根据作者用户名搜索该作者的所有文章")
    public Map<String, Object> searchPostsByAuthor(
            @ToolParam(description = "作者用户名", required = true) String username,
            @ToolParam(description = "返回结果数量限制", required = false) Integer limit) {

        log.info("Tool Calling: search_posts_by_author - 作者: {}, 限制: {}", username, limit);

        int size = limit != null ? limit : 10;

        // 根据用户名查询作者
        Users author = userMapper.findByUsername(username).orElse(null);
        List<Posts> posts = new ArrayList<>();

        // 作者存在，才查询他的文章
        if (author != null) {
            posts = postMapper.selectList(new LambdaQueryWrapper<Posts>()
                    .eq(Posts::getAuthorId, author.getId())       // 匹配作者ID
                    .eq(Posts::getStatus, Status.PUBLISHED)       // 只查已发布
                    .orderByDesc(Posts::getCreatedAt));           // 按发布时间倒序
        }

        if (posts.size() > size) {
            posts = posts.subList(0, size);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("author", username);
        result.put("total", posts.size());
        result.put("posts", posts);
        return result;
    }

    /**
     * AI工具：高级搜索（关键词+标签+作者+状态组合查询）
     */
    @Tool(name = "advanced_search", description = "支持多条件组合搜索文章，可同时指定关键词、标签、作者和状态")
    public Map<String, Object> advancedSearch(
            @ToolParam(description = "搜索关键词（可选）", required = false) String keyword,
            @ToolParam(description = "标签名称（可选）", required = false) String tagName,
            @ToolParam(description = "作者用户名（可选）", required = false) String author,
            @ToolParam(description = "文章状态（可选）：PUBLISHED-已发布", required = false) String status,
            @ToolParam(description = "返回结果数量限制", required = false) Integer limit) {

        log.info("Tool Calling: advanced_search - 关键词: {}, 标签: {}, 作者: {}, 状态: {}",
                keyword, tagName, author, status);

        int size = limit != null ? limit : 10;

        // 先查询所有已发布文章
        List<Posts> posts = postMapper.selectList(new LambdaQueryWrapper<Posts>()
                .eq(Posts::getStatus, Status.PUBLISHED)
                .orderByDesc(Posts::getCreatedAt));

        // 有关键词：过滤标题/内容包含关键词
        if (keyword != null && !keyword.isEmpty()) {
            posts = posts.stream()
                    .filter(p -> p.getTitle().contains(keyword) || p.getContent().contains(keyword))
                    .toList();
        }

        // 有标签：过滤包含该标签的文章
        if (tagName != null && !tagName.isEmpty()) {
            List<Posts> tagPosts = postMapper.findByTagName(Status.PUBLISHED, tagName);
            posts = posts.stream()
                    .filter(p -> tagPosts.stream().anyMatch(tp -> tp.getId().equals(p.getId())))
                    .toList();
        }

        // 有作者：过滤该作者的文章
        if (author != null && !author.isEmpty()) {
            Users authorUser = userMapper.findByUsername(author).orElse(null);
            if (authorUser != null) {
                posts = posts.stream()
                        .filter(p -> p.getAuthorId().equals(authorUser.getId()))
                        .toList();
            }
        }

        // 截取限制条数
        if (posts.size() > size) {
            posts = posts.subList(0, size);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("keyword", keyword != null ? keyword : "");
        result.put("tagName", tagName != null ? tagName : "");
        result.put("author", author != null ? author : "");
        result.put("status", status != null ? status : "PUBLISHED");
        result.put("total", posts.size());
        result.put("posts", posts);
        return result;
    }

    /**
     * AI工具：获取系统所有标签
     */
    @Tool(name = "get_all_tags", description = "获取系统中所有的文章标签")
    public Map<String, Object> getAllTags(
            @ToolParam(description = "返回数量限制", required = false) Integer limit) {

        log.info("Tool Calling: get_all_tags - 限制: {}", limit);

        // 查询所有标签
        List<Tags> tags = tagMapper.selectList(null);

        // 限制条数
        int size = limit != null && limit < tags.size() ? limit : tags.size();

        Map<String, Object> result = new HashMap<>();
        result.put("total", tags.size());
        result.put("returned", size);
        result.put("tags", tags.subList(0, size));
        return result;
    }

    /**
     * AI工具：获取热门标签
     */
    @Tool(name = "get_popular_tags", description = "获取使用频率最高的标签")
    public Map<String, Object> getPopularTags(
            @ToolParam(description = "返回数量限制", required = false) Integer limit) {

        log.info("Tool Calling: get_popular_tags - 限制: {}", limit);

        int size = limit != null ? limit : 10;

        // 查询最新创建的标签（模拟热门）
        List<Tags> tags = tagMapper.selectList(new LambdaQueryWrapper<Tags>()
                .orderByDesc(Tags::getCreatedAt)
                .last("LIMIT " + size));

        Map<String, Object> result = new HashMap<>();
        result.put("total", tags.size());
        result.put("tags", tags);
        return result;
    }

    /**
     * AI工具：根据关键词搜索标签
     */
    @Tool(name = "search_tags", description = "根据关键词搜索标签")
    public Map<String, Object> searchTags(
            @ToolParam(description = "搜索关键词", required = true) String keyword,
            @ToolParam(description = "返回数量限制", required = false) Integer limit) {

        log.info("Tool Calling: search_tags - 关键词: {}, 限制: {}", keyword, limit);

        int size = limit != null ? limit : 10;

        // 模糊查询标签名
        List<Tags> tags = tagMapper.selectList(new LambdaQueryWrapper<Tags>()
                .like(Tags::getName, keyword));

        int returned = Math.min(size, tags.size());

        Map<String, Object> result = new HashMap<>();
        result.put("keyword", keyword);
        result.put("total", tags.size());
        result.put("returned", returned);
        result.put("tags", tags.subList(0, returned));
        return result;
    }

    /**
     * AI工具：根据文章ID，获取相似推荐文章（按标签相似度）
     */
    @Tool(name = "get_related_posts", description = "获取与指定文章相关的其他文章，基于标签相似度")
    public Map<String, Object> getRelatedPosts(
            @ToolParam(description = "文章ID", required = true) Long postId,
            @ToolParam(description = "返回数量限制", required = false) Integer limit) {

        log.info("Tool Calling: get_related_posts - 文章ID: {}, 限制: {}", postId, limit);

        int size = limit != null ? limit : 5;

        // 查询原文章是否存在
        Posts post = postMapper.selectById(postId);
        if (post == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "文章不存在");
            return result;
        }

        // 获取当前文章的所有标签
        List<Tags> currentTags = postMapper.selectTagsByPostId(postId);
        List<String> currentTagNames = currentTags.stream().map(Tags::getName).toList();

        // 查询所有已发布文章
        List<Posts> allPosts = postMapper.selectList(new LambdaQueryWrapper<Posts>()
                .eq(Posts::getStatus, Status.PUBLISHED)
                .orderByDesc(Posts::getCreatedAt));

        // 筛选：排除自己 + 按相同标签数量排序（越多越相关）
        List<Posts> relatedPosts = allPosts.stream()
                .filter(p -> !p.getId().equals(postId))
                .sorted((p1, p2) -> {
                    // 计算文章1与当前文章的相同标签数
                    List<Tags> tags1 = postMapper.selectTagsByPostId(p1.getId());
                    int score1 = (int) tags1.stream().filter(t -> currentTagNames.contains(t.getName())).count();

                    // 计算文章2与当前文章的相同标签数
                    List<Tags> tags2 = postMapper.selectTagsByPostId(p2.getId());
                    int score2 = (int) tags2.stream().filter(t -> currentTagNames.contains(t.getName())).count();

                    // 降序：分数高的排前面
                    return Integer.compare(score2, score1);
                })
                .limit(size)
                .toList();

        // 封装推荐结果
        Map<String, Object> result = new HashMap<>();
        result.put("originalPostId", postId);
        result.put("originalPostTitle", post.getTitle());
        result.put("total", relatedPosts.size());
        result.put("relatedPosts", relatedPosts);
        return result;
    }
}