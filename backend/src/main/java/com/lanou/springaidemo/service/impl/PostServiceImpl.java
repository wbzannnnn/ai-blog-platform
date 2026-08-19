package com.lanou.springaidemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanou.springaidemo.dto.request.PostCreateRequest;
import com.lanou.springaidemo.dto.response.PostResponse;
import com.lanou.springaidemo.dto.response.TagResponse;
import com.lanou.springaidemo.dto.response.UserResponse;
import com.lanou.springaidemo.entity.Posts;
import com.lanou.springaidemo.entity.Tags;
import com.lanou.springaidemo.entity.Users;
import com.lanou.springaidemo.enums.ModerationStatus;
import com.lanou.springaidemo.enums.Status;
import com.lanou.springaidemo.exception.BusinessException;
import com.lanou.springaidemo.mapper.CommentsMapper;
import com.lanou.springaidemo.mapper.PostsMapper;
import com.lanou.springaidemo.mapper.TagsMapper;
import com.lanou.springaidemo.mapper.UsersMapper;
import com.lanou.springaidemo.service.PostService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author lanou
 * @since 2026-06-08
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl extends ServiceImpl<PostsMapper, Posts> implements PostService {

    // 文章数据库操作Mapper
    private final PostsMapper postsMapper;
    // 标签数据库操作Mapper
    private final TagsMapper tagsMapper;
    // 评论数据库操作Mapper
    private final CommentsMapper commentMapper;
    // 用户数据库操作Mapper
    private final UsersMapper userMapper;
    // AI服务（内容审核、生成）
    private final AiService aiService;

    /**
     * 实体转换工具方法：Posts → PostResponse
     * 封装作者信息、标签列表、评论数量，返回给前端使用
     */
    private PostResponse toPostResponse(Posts post) {
        // 查询文章作者信息
        Users author = userMapper.selectById(post.getAuthorId());
        // 转换为用户响应DTO，空值处理
        UserResponse authorResponse = author != null ? UserResponse.fromEntity(author) : null;
        // 查询文章关联的所有标签
        List<Tags> tags = postsMapper.selectTagsByPostId(post.getId());
        // 标签实体转换为响应DTO
        List<TagResponse> tagResponses = tags.stream().map(TagResponse::fromEntity).toList();
        // 查询当前文章的评论总数
        long commentCount = commentMapper.countByPostId(post.getId());
        // 组装并返回最终响应对象
        return PostResponse.fromEntity(post, authorResponse, tagResponses, commentCount);
    }

    /**
     * 创建文章
     * 包含：实体构建、状态处理、AI审核、标签关联、数据库保存
     */
    @Transactional
    @Override
    public PostResponse createPost(Users author, PostCreateRequest request) {
        // 构建文章实体，设置初始值
        Posts post = Posts.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .summary(request.getSummary())
                .isAiGenerated(request.getIsAiGenerated() != null ? request.getIsAiGenerated() : false)
                .authorId(author.getId())
                .likeCount(0)
                .viewCount(0)
                .status(Status.DRAFT)
                .moderationStatus(ModerationStatus.PENDING)
                .createdAt(System.currentTimeMillis())
                .updatedAt(System.currentTimeMillis())
                .build();

        // 根据请求的状态，处理发布与AI审核逻辑
        if (request.getStatus() != null) {
            // 提交待审核状态
            if (request.getStatus() == Status.PENDING_REVIEW) {
                post.setStatus(Status.PENDING_REVIEW);
                post.setModerationStatus(ModerationStatus.PENDING);
                // 调用AI进行内容审核
                AiService.ModerationResult result = aiService.moderateContent(request.getContent());
                if (result.approved()) {
                    post.setModerationStatus(ModerationStatus.APPROVED);
                    post.setModerationResult("AI预审通过");
                } else {
                    post.setModerationStatus(ModerationStatus.REJECTED);
                    post.setModerationResult(result.reason());
                }
            }
            // 直接发布状态（需完整审核）
            else if (request.getStatus() == Status.PUBLISHED) {
                AiService.ModerationResult result = aiService.moderateContent(request.getContent());
                if (result.approved()) {
                    post.setStatus(Status.PUBLISHED);
                    post.setModerationStatus(ModerationStatus.APPROVED);
                    post.setModerationResult("审核通过");
                } else {
                    post.setStatus(Status.DRAFT);
                    post.setModerationStatus(ModerationStatus.REJECTED);
                    post.setModerationResult(result.reason());
                }
            }
            // 保存为草稿
            else if (request.getStatus() == Status.DRAFT) {
                post.setStatus(Status.DRAFT);
            }
        }

        // 插入文章到数据库
        postsMapper.insert(post);

        // 处理文章标签：不存在则创建，存在则直接关联
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            for (String tagName : request.getTags()) {
                Tags tag = tagsMapper.findByName(tagName).orElseGet(() -> {
                    // 标签不存在，创建新标签
                    Tags newTag = Tags.builder()
                            .name(tagName)
                            .createdAt(System.currentTimeMillis())
                            .build();
                    tagsMapper.insert(newTag);
                    return newTag;
                });
                // 建立文章与标签的关联关系
                postsMapper.insertPostTag(post.getId(), tag.getId());
            }
        }

        log.info("文章创建成功: {}", post.getTitle());
        return toPostResponse(post);
    }

    /**
     * 更新文章
     * 包含权限校验、内容更新、标签重置、状态与审核处理
     */
    @Transactional
    @Override
    public PostResponse updatePost(Long id, Users author, PostCreateRequest request) {
        // 查询文章是否存在
        Posts post = postsMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }

        // 权限校验：只能修改自己的文章或管理员可修改
        if (!post.getAuthorId().equals(author.getId()) && !author.getRole().equals(Users.Role.ADMIN)) {
            throw new BusinessException(403, "无权修改此文章");
        }

        // 更新基础内容字段
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());

        // 可选字段：有值才更新
        if (request.getSummary() != null) {
            post.setSummary(request.getSummary());
        }
        if (request.getIsAiGenerated() != null) {
            post.setIsAiGenerated(request.getIsAiGenerated());
        }

        // 更新标签：先删除旧关联，再重新添加新关联
        if (request.getTags() != null) {
            postsMapper.deletePostTagsByPostId(post.getId());
            for (String tagName : request.getTags()) {
                Tags tag = tagsMapper.findByName(tagName).orElseGet(() -> {
                    Tags newTag = Tags.builder()
                            .name(tagName)
                            .createdAt(System.currentTimeMillis())
                            .build();
                    tagsMapper.insert(newTag);
                    return newTag;
                });
                postsMapper.insertPostTag(post.getId(), tag.getId());
            }
        }

        // 处理文章状态变更 + 重新审核
        if (request.getStatus() != null) {
            Status newStatus = request.getStatus();

            if (newStatus == Status.PENDING_REVIEW) {
                // 提交审核
                post.setStatus(Status.PENDING_REVIEW);
                post.setModerationStatus(ModerationStatus.PENDING);
                AiService.ModerationResult result = aiService.moderateContent(post.getContent());
                if (result.approved()) {
                    post.setModerationStatus(ModerationStatus.APPROVED);
                    post.setModerationResult("AI预审通过");
                } else {
                    post.setModerationStatus(ModerationStatus.REJECTED);
                    post.setModerationResult(result.reason());
                }
            } else if (newStatus == Status.PUBLISHED) {
                // 直接发布，必须审核通过
                if (post.getStatus() != Status.PUBLISHED) {
                    AiService.ModerationResult result = aiService.moderateContent(post.getContent());
                    if (result.approved()) {
                        post.setStatus(Status.PUBLISHED);
                        post.setModerationStatus(ModerationStatus.APPROVED);
                        post.setModerationResult("审核通过");
                    } else {
                        post.setModerationStatus(ModerationStatus.REJECTED);
                        post.setModerationResult(result.reason());
                        throw new BusinessException(400, "内容审核未通过: " + result.reason());
                    }
                }
            } else if (newStatus == Status.DRAFT) {
                // 设为草稿
                post.setStatus(Status.DRAFT);
                post.setModerationStatus(ModerationStatus.PENDING);
            } else if (newStatus == Status.ARCHIVED) {
                // 设为归档
                post.setStatus(Status.ARCHIVED);
            }
        }

        // 更新时间
        post.setUpdatedAt(System.currentTimeMillis());
        postsMapper.updateById(post);

        return toPostResponse(post);
    }

    /**
     * 根据ID查询文章详情
     * 浏览量自动+1
     */
    @Override
    public PostResponse getPostById(Long id) {
        Posts post = postsMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }

        // 浏览量+1
        post.setViewCount(post.getViewCount() + 1);
        postsMapper.updateById(post);

        return toPostResponse(post);
    }

    /**
     * 分页查询已发布文章
     */
    @Override
    public List<PostResponse> getPublishedPosts(int page, int size) {
        List<Posts> posts = postsMapper.findByStatusOrderByCreatedAtDesc(Status.PUBLISHED);
        int start = page * size;
        if (start >= posts.size()) {
            return new ArrayList<>();
        }
        int end = Math.min(start + size, posts.size());
        return posts.subList(start, end).stream().map(this::toPostResponse).collect(Collectors.toList());
    }

    /**
     * 统计已发布文章总数
     */
    @Override
    public long countPublishedPosts() {
        return postsMapper.countByStatus(Status.PUBLISHED);
    }

    /**
     * 根据作者ID分页查询文章
     */
    @Override
    public List<PostResponse> getPostsByAuthor(Long authorId, int page, int size) {
        List<Posts> posts = postsMapper.findByStatusAndAuthorId(Status.PUBLISHED, authorId);
        int start = page * size;
        if (start >= posts.size()) {
            return new ArrayList<>();
        }
        int end = Math.min(start + size, posts.size());
        return posts.subList(start, end).stream().map(this::toPostResponse).collect(Collectors.toList());
    }

    /**
     * 统计指定作者的文章数量
     */
    @Override
    public long countPostsByAuthor(Long authorId) {
        return postsMapper.selectCount(new LambdaQueryWrapper<Posts>()
                .eq(Posts::getStatus, Status.PUBLISHED)
                .eq(Posts::getAuthorId, authorId));
    }

    /**
     * 关键词搜索文章（标题+内容）
     */
    @Override
    public List<PostResponse> searchPosts(String keyword, int page, int size) {
        List<Posts> posts = postsMapper.searchByKeyword(Status.PUBLISHED, keyword);
        int start = page * size;
        if (start >= posts.size()) {
            return new ArrayList<>();
        }
        int end = Math.min(start + size, posts.size());
        return posts.subList(start, end).stream().map(this::toPostResponse).collect(Collectors.toList());
    }

    @Override
    public long countSearchPosts(String keyword) {
        return postsMapper.countByKeyword(Status.PUBLISHED, keyword);
    }

    /**
     * 根据标签列表查询文章（自动去重）
     */
    @Override
    public List<PostResponse> getPostsByTags(List<String> tags, int page, int size) {
        List<Posts> allPosts = new ArrayList<>();
        for (String tagName : tags) {
            List<Posts> tagPosts = postsMapper.findByTagName(Status.PUBLISHED, tagName);
            allPosts.addAll(tagPosts);
        }
        // 去重
        allPosts = allPosts.stream().distinct().collect(Collectors.toList());

        int start = page * size;
        if (start >= allPosts.size()) {
            return new ArrayList<>();
        }
        int end = Math.min(start + size, allPosts.size());
        return allPosts.subList(start, end).stream().map(this::toPostResponse).collect(Collectors.toList());
    }

    /**
     * 分页查询待审核文章
     */
    @Override
    public List<PostResponse> getPendingPosts(int page, int size) {
        List<Posts> posts = postsMapper.findByModerationStatus(ModerationStatus.PENDING);
        int start = page * size;
        if (start >= posts.size()) {
            return new ArrayList<>();
        }
        int end = Math.min(start + size, posts.size());
        return posts.subList(start, end).stream().map(this::toPostResponse).collect(Collectors.toList());
    }

    /**
     * 统计待审核文章数量
     */
    @Override
    public long countPendingPosts() {
        return postsMapper.selectCount(new LambdaQueryWrapper<Posts>()
                .eq(Posts::getModerationStatus, ModerationStatus.PENDING));
    }

    /**
     * 删除文章（权限校验）
     * 先删标签关联，再删文章
     */
    @Override
    @Transactional
    public void deletePost(Long id, Users author) {
        Posts post = postsMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }

        // 权限校验
        if (!post.getAuthorId().equals(author.getId()) && !author.getRole().equals(Users.Role.ADMIN)) {
            throw new BusinessException(403, "无权删除此文章");
        }

        // 删除关联标签
        postsMapper.deletePostTagsByPostId(id);
        // 删除文章
        postsMapper.deleteById(id);
        log.info("文章删除成功: {}", id);
    }

    /**
     * 管理员审核通过：发布文章
     */
    @Override
    @Transactional
    public PostResponse approvePost(Long id) {
        Posts post = postsMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }

        // 设置状态为已发布、审核通过
        post.setModerationStatus(ModerationStatus.APPROVED);
        post.setStatus(Status.PUBLISHED);
        post.setModerationResult("审核通过");
        post.setUpdatedAt(System.currentTimeMillis());

        postsMapper.updateById(post);
        return toPostResponse(post);
    }

    /**
     * 管理员审核拒绝：退回草稿
     */
    @Override
    @Transactional
    public PostResponse rejectPost(Long id, String reason) {
        Posts post = postsMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }

        // 设置状态为已拒绝、草稿
        post.setModerationStatus(ModerationStatus.REJECTED);
        post.setStatus(Status.DRAFT);
        post.setModerationResult(reason);
        post.setUpdatedAt(System.currentTimeMillis());

        postsMapper.updateById(post);
        return toPostResponse(post);
    }

    /**
     * 获取最新10条已发布文章
     */
    @Override
    public List<PostResponse> getLatestPosts() {
        List<Posts> posts = postsMapper.findTop10ByStatusOrderByCreatedAtDesc(Status.PUBLISHED);
        return posts.stream().map(this::toPostResponse).collect(Collectors.toList());
    }
}