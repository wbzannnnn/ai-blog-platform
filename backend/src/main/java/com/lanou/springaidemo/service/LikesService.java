package com.lanou.springaidemo.service;

import com.lanou.springaidemo.entity.Comments;
import com.lanou.springaidemo.entity.Likes;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lanou.springaidemo.entity.Posts;
import com.lanou.springaidemo.entity.Users;
import com.lanou.springaidemo.exception.BusinessException;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author lanou
 * @since 2026-06-08
 */
public interface LikesService extends IService<Likes> {
    void likePost(Users user, Long postId);

    void unlikePost(Users user, Long postId);

    void likeComment(Users user, Long commentId);

    void unlikeComment(Users user, Long commentId);

    boolean isPostLiked(Users user, Long postId);

    boolean isCommentLiked(Users user, Long commentId);

}
