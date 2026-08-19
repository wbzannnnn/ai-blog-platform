package com.lanou.springaidemo.service.impl;

import com.lanou.springaidemo.entity.Comments;
import com.lanou.springaidemo.entity.Likes;
import com.lanou.springaidemo.entity.Posts;
import com.lanou.springaidemo.entity.Users;
import com.lanou.springaidemo.exception.BusinessException;
import com.lanou.springaidemo.mapper.CommentsMapper;
import com.lanou.springaidemo.mapper.LikesMapper;
import com.lanou.springaidemo.mapper.PostsMapper;
import com.lanou.springaidemo.service.LikesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class LikesServiceImpl extends ServiceImpl<LikesMapper, Likes> implements LikesService {
    private final LikesMapper likeMapper;
    private final PostsMapper postMapper;
    private final CommentsMapper commentMapper;

    @Transactional
    @Override
    public void likePost(Users user, Long postId) {
        Posts post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(404, "文章不存在");
        }

        if (likeMapper.existsByUserIdAndPostId(user.getId(), postId)) {
            // 已点赞，静默返回（幂等操作，不报错）
            log.debug("文章已点赞，跳过: {}", postId);
            return;
        }

        Likes like = Likes.builder()
                .userId(user.getId())
                .postId(postId)
                .createdAt(System.currentTimeMillis())
                .build();

        likeMapper.insert(like);
        post.setLikeCount(post.getLikeCount() + 1);
        postMapper.updateById(post);

        log.info("文章点赞成功: {}", postId);
    }

    @Transactional
    @Override
    public void unlikePost(Users user, Long postId) {
        if (!likeMapper.existsByUserIdAndPostId(user.getId(), postId)) {
            // 未点赞，静默返回
            log.debug("文章未点赞，跳过取消: {}", postId);
            return;
        }

        likeMapper.deleteByUserIdAndPostId(user.getId(), postId);

        Posts post = postMapper.selectById(postId);
        if (post != null && post.getLikeCount() > 0) {
            post.setLikeCount(post.getLikeCount() - 1);
            postMapper.updateById(post);
        }

        log.info("文章取消点赞: {}", postId);
    }

    @Transactional
    @Override
    public void likeComment(Users user, Long commentId) {
        Comments comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(404, "评论不存在");
        }

        if (likeMapper.existsByUserIdAndCommentId(user.getId(), commentId)) {
            // 已点赞，静默返回（幂等操作，不报错）
            log.debug("评论已点赞，跳过: {}", commentId);
            return;
        }

        Likes like = Likes.builder()
                .userId(user.getId())
                .commentId(commentId)
                .createdAt(System.currentTimeMillis())
                .build();

        likeMapper.insert(like);
        comment.setLikeCount(comment.getLikeCount() + 1);
        commentMapper.updateById(comment);

        log.info("评论点赞成功: {}", commentId);
    }

    @Transactional
    @Override
    public void unlikeComment(Users user, Long commentId) {
        if (!likeMapper.existsByUserIdAndCommentId(user.getId(), commentId)) {
            // 未点赞，静默返回
            log.debug("评论未点赞，跳过取消: {}", commentId);
            return;
        }

        likeMapper.deleteByUserIdAndCommentId(user.getId(), commentId);

        Comments comment = commentMapper.selectById(commentId);
        if (comment != null && comment.getLikeCount() > 0) {
            comment.setLikeCount(comment.getLikeCount() - 1);
            commentMapper.updateById(comment);
        }

        log.info("评论取消点赞: {}", commentId);
    }

    @Override
    public boolean isPostLiked(Users user, Long postId) {
        return likeMapper.existsByUserIdAndPostId(user.getId(), postId);
    }

    @Override
    public boolean isCommentLiked(Users user, Long commentId) {
        return likeMapper.existsByUserIdAndCommentId(user.getId(), commentId);
    }
}
