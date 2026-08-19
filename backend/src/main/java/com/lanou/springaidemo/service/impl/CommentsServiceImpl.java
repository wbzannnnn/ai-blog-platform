package com.lanou.springaidemo.service.impl;

import com.lanou.springaidemo.dto.request.CommentCreateRequest;
import com.lanou.springaidemo.dto.response.CommentResponse;
import com.lanou.springaidemo.dto.response.UserResponse;
import com.lanou.springaidemo.entity.Comments;
import com.lanou.springaidemo.entity.Posts;
import com.lanou.springaidemo.entity.Users;
import com.lanou.springaidemo.enums.Status;
import com.lanou.springaidemo.exception.BusinessException;
import com.lanou.springaidemo.mapper.CommentsMapper;
import com.lanou.springaidemo.mapper.PostsMapper;
import com.lanou.springaidemo.mapper.UsersMapper;
import com.lanou.springaidemo.service.CommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author lanou
 * @since 2026-06-08
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentsServiceImpl extends ServiceImpl<CommentsMapper, Comments> implements CommentsService {
    private final CommentsMapper commentMapper;
    private final PostsMapper postMapper;
    private final UsersMapper userMapper;

    /**
     * 创建评论（发布评论）
     * @param author  当前登录的用户（评论发布者）
     * @param request 前端传递的评论参数
     * @return 封装好的评论响应对象
     */
    @Transactional  // 开启事务，保证数据一致性
    @Override
    public CommentResponse createComment(Users author, CommentCreateRequest request) {
        // 1. 根据文章ID查询文章是否存在
        Posts post = postMapper.selectById(request.getPostId());
        if (post == null) {
            // 文章不存在，抛出业务异常
            throw new BusinessException(404, "文章不存在");
        }

        // 2. 只有已发布的文章才能评论
        if (post.getStatus() != Status.PUBLISHED) {
            throw new BusinessException(400, "只能对已发布的文章评论");
        }

        // 3. 如果是回复评论（parentId 不为空），校验父评论是否存在
        Comments parent = null;
        if (request.getParentId() != null) {
            parent = commentMapper.selectById(request.getParentId());
            if (parent == null) {
                throw new BusinessException(404, "父评论不存在");
            }
        }

        // 4. 使用建造者模式构建评论实体对象
        Comments comment = Comments.builder()
                .content(request.getContent())        // 评论内容
                .postId(request.getPostId())          // 所属文章ID
                .authorId(author.getId())             // 评论作者ID
                .parentId(request.getParentId())      // 父评论ID（可为空）
                .likeCount(0)                         // 初始点赞数为0
                .createdAt(System.currentTimeMillis()) // 创建时间戳
                .updatedAt(System.currentTimeMillis()) // 更新时间戳
                .build();

        // 5. 插入数据库
        commentMapper.insert(comment);

        // 5.1 更新文章评论数
        post.setCommentCount(post.getCommentCount() + 1);
        postMapper.updateById(post);

        // 打印日志
        log.info("评论创建成功: {}", comment.getId());

        // 6. 转换为前端需要的响应格式并返回
        return CommentResponse.fromEntity(comment, UserResponse.fromEntity(author));
    }

    /**
     * 根据文章ID查询该文章下的所有评论（含嵌套回复）
     * @param postId 文章ID
     * @return 评论响应列表（树形结构，含 replies）
     */
    @Override
    public List<CommentResponse> getCommentsByPost(Long postId) {
        // 1. 校验文章是否存在
        Posts post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }

        // 2. 查询所有一级评论（parentId 为 null），按创建时间倒序
        List<Comments> topLevelComments = commentMapper.findByPostIdAndParentIsNullOrderByCreatedAtDesc(postId);

        // 3. 递归组装评论树
        return topLevelComments.stream()
                .map(comment -> buildCommentTree(comment))
                .collect(Collectors.toList());
    }

    /**
     * 递归构建评论树：把一个评论实体 + 它的所有子孙回复 → CommentResponse
     */
    private CommentResponse buildCommentTree(Comments comment) {
        Users author = userMapper.selectById(comment.getAuthorId());
        UserResponse authorResponse = author != null ? UserResponse.fromEntity(author) : null;

        // 查子回复
        List<Comments> children = commentMapper.findByParentId(comment.getId());
        List<CommentResponse> childResponses = children.stream()
                .map(this::buildCommentTree)  // 递归处理孙子回复
                .collect(Collectors.toList());

        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .author(authorResponse)
                .parentId(comment.getParentId())
                .replies(childResponses)
                .likeCount(comment.getLikeCount())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    /**
     * 删除评论
     * 权限控制：只能删除自己的评论 或 管理员可删任意评论
     * @param id   要删除的评论ID
     * @param user 当前登录用户
     */
    @Transactional  // 开启事务
    @Override
    public void deleteComment(Long id, Users user) {
        // 1. 查询要删除的评论是否存在
        Comments comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }

        // 2. 权限校验：不是作者 且 不是管理员 → 无权删除
        if (!comment.getAuthorId().equals(user.getId()) && !user.getRole().equals(Users.Role.ADMIN)) {
            throw new BusinessException(403, "无权删除此评论");
        }

        // 3. 执行删除
        commentMapper.deleteById(id);
        log.info("评论删除成功: {}", id);
    }
}
