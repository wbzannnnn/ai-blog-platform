package com.lanou.springaidemo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lanou.springaidemo.dto.request.PostCreateRequest;
import com.lanou.springaidemo.dto.response.PostResponse;
import com.lanou.springaidemo.entity.Posts;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lanou.springaidemo.entity.Tags;
import com.lanou.springaidemo.entity.Users;
import com.lanou.springaidemo.enums.ModerationStatus;
import com.lanou.springaidemo.enums.Status;
import com.lanou.springaidemo.exception.BusinessException;
import com.lanou.springaidemo.service.impl.AiService;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
public interface PostService extends IService<Posts> {

    PostResponse createPost(Users author, PostCreateRequest request);

    PostResponse updatePost(Long id, Users author, PostCreateRequest request);

    PostResponse getPostById(Long id);

    List<PostResponse> getPublishedPosts(int page, int size);

    long countPublishedPosts();

    List<PostResponse> getPostsByAuthor(Long authorId, int page, int size);

    long countPostsByAuthor(Long authorId);

    List<PostResponse> searchPosts(String keyword, int page, int size);

    long countSearchPosts(String keyword);

    List<PostResponse> getPostsByTags(List<String> tags, int page, int size);

    List<PostResponse> getPendingPosts(int page, int size);

    long countPendingPosts();

    void deletePost(Long id, Users author);

    PostResponse approvePost(Long id);

    PostResponse rejectPost(Long id, String reason);

    List<PostResponse> getLatestPosts();
}
