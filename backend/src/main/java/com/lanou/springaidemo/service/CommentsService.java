package com.lanou.springaidemo.service;

import com.lanou.springaidemo.dto.request.CommentCreateRequest;
import com.lanou.springaidemo.dto.response.CommentResponse;
import com.lanou.springaidemo.dto.response.UserResponse;
import com.lanou.springaidemo.entity.Comments;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lanou.springaidemo.entity.Posts;
import com.lanou.springaidemo.entity.Users;
import com.lanou.springaidemo.enums.Status;
import com.lanou.springaidemo.exception.BusinessException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author lanou
 * @since 2026-06-08
 */
public interface CommentsService extends IService<Comments> {

    CommentResponse createComment(Users author, CommentCreateRequest request);

    List<CommentResponse> getCommentsByPost(Long postId);

    void deleteComment(Long id, Users user);
}
