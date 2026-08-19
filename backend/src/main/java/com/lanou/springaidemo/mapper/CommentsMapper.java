package com.lanou.springaidemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lanou.springaidemo.entity.Comments;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author lanou
 * @since 2026-06-08
 */
@Mapper
public interface CommentsMapper extends BaseMapper<Comments> {

    @Select("SELECT * FROM t_comments WHERE post_id = #{postId} AND parent_id IS NULL ORDER BY created_at DESC")
    List<Comments> findByPostIdAndParentIsNullOrderByCreatedAtDesc(@Param("postId") Long postId);

    @Select("SELECT * FROM t_comments WHERE parent_id = #{parentId} ORDER BY created_at ASC")
    List<Comments> findByParentId(@Param("parentId") Long parentId);

    @Select("SELECT COUNT(*) FROM t_comments WHERE post_id = #{postId}")
    long countByPostId(@Param("postId") Long postId);

}
