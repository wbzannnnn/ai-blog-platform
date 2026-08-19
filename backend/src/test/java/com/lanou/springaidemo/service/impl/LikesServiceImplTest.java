package com.lanou.springaidemo.service.impl;

import com.lanou.springaidemo.entity.Likes;
import com.lanou.springaidemo.entity.Posts;
import com.lanou.springaidemo.entity.Users;
import com.lanou.springaidemo.mapper.CommentsMapper;
import com.lanou.springaidemo.mapper.LikesMapper;
import com.lanou.springaidemo.mapper.PostsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LikesServiceImplTest {

    private LikesMapper likesMapper;
    private PostsMapper postsMapper;
    private LikesServiceImpl service;
    private Users user;

    @BeforeEach
    void setUp() {
        likesMapper = mock(LikesMapper.class);
        postsMapper = mock(PostsMapper.class);
        service = new LikesServiceImpl(likesMapper, postsMapper, mock(CommentsMapper.class));
        user = Users.builder().id(8L).build();
    }

    @Test
    void likePostCreatesRelationAndIncrementsCount() {
        Posts post = Posts.builder().id(5L).likeCount(2).build();
        when(postsMapper.selectById(5L)).thenReturn(post);
        when(likesMapper.existsByUserIdAndPostId(8L, 5L)).thenReturn(false);

        service.likePost(user, 5L);

        assertEquals(3, post.getLikeCount());
        verify(likesMapper).insert(any(Likes.class));
        verify(postsMapper).updateById(post);
    }

    @Test
    void repeatedLikeDoesNotIncrementAgain() {
        Posts post = Posts.builder().id(5L).likeCount(2).build();
        when(postsMapper.selectById(5L)).thenReturn(post);
        when(likesMapper.existsByUserIdAndPostId(8L, 5L)).thenReturn(true);

        service.likePost(user, 5L);

        assertEquals(2, post.getLikeCount());
        verify(likesMapper, never()).insert(any(Likes.class));
        verify(postsMapper, never()).updateById(any(Posts.class));
    }

    @Test
    void unlikePostDeletesRelationAndDecrementsCount() {
        Posts post = Posts.builder().id(5L).likeCount(2).build();
        when(likesMapper.existsByUserIdAndPostId(8L, 5L)).thenReturn(true);
        when(postsMapper.selectById(5L)).thenReturn(post);

        service.unlikePost(user, 5L);

        assertEquals(1, post.getLikeCount());
        verify(likesMapper).deleteByUserIdAndPostId(8L, 5L);
        verify(postsMapper).updateById(post);
    }
}
