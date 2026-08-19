package com.lanou.springaidemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lanou.springaidemo.entity.Likes;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author lanou
 * @since 2026-06-08
 */
@Mapper
public interface LikesMapper extends BaseMapper<Likes> {
    /**
     * 查询【用户是否已经点赞了某篇文章】
     *
     * @param userId 用户ID
     * @param postId 文章ID
     * @return true=已点赞，false=未点赞
     */
    @Select("SELECT COUNT(*) FROM t_likes WHERE user_id = #{userId} AND post_id = #{postId}")
    boolean existsByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 查询【用户是否已经点赞了某条评论】
     *
     * @param userId    用户ID
     * @param commentId 评论ID
     * @return true=已点赞，false=未点赞
     */
    @Select("SELECT COUNT(*) FROM t_likes WHERE user_id = #{userId} AND comment_id = #{commentId}")
    boolean existsByUserIdAndCommentId(@Param("userId") Long userId, @Param("commentId") Long commentId);

    /**
     * 取消点赞：根据 用户ID + 文章ID 删除点赞记录
     *
     * @param userId 用户ID
     * @param postId 文章ID
     */
    @Delete("DELETE FROM t_likes WHERE user_id = #{userId} AND post_id = #{postId}")
    void deleteByUserIdAndPostId(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * 取消点赞：根据 用户ID + 评论ID 删除点赞记录
     */
    @Delete("DELETE FROM t_likes WHERE user_id = #{userId} AND comment_id = #{commentId}")
    void deleteByUserIdAndCommentId(@Param("userId") Long userId, @Param("commentId") Long commentId);
}
