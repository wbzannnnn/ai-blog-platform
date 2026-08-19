package com.lanou.springaidemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lanou.springaidemo.entity.Tags;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author lanou
 * @since 2026-06-08
 */
@Mapper
public interface TagsMapper extends BaseMapper<Tags> {
    @Select("SELECT * FROM t_tags WHERE name = #{name}")
    Optional<Tags> findByName(@Param("name") String name);

    @Select("SELECT * FROM t_tags WHERE name LIKE CONCAT('%', #{keyword}, '%')")
    List<Tags> findByNameContaining(@Param("keyword") String keyword);

    @Select("SELECT t.* FROM t_tags t JOIN t_post_tags pt ON t.id = pt.tag_id GROUP BY t.id ORDER BY COUNT(pt.post_id) DESC LIMIT #{limit}")
    List<Tags> findTopByPostCount(@Param("limit") int limit);

    @Select("""
        SELECT t.id as tagId, t.name as tagName,
               COUNT(DISTINCT p.id) as articleCount,
               COALESCE(SUM(p.view_count), 0) as totalViews,
               COALESCE(SUM(p.like_count), 0) as totalLikes,
               COALESCE(SUM(p.comment_count), 0) as totalComments
        FROM t_tags t
        LEFT JOIN t_post_tags pt ON t.id = pt.tag_id
        LEFT JOIN t_posts p ON pt.post_id = p.id AND p.status = 'PUBLISHED'
        GROUP BY t.id, t.name
        ORDER BY articleCount DESC, totalViews DESC
    """)
    List<Map<String, Object>> getTagAnalytics();

    @Select("""
        SELECT t.id as tagId, t.name as tagName,
               FROM_UNIXTIME(p.created_at/1000, '%Y-%m-%d') as date,
               COUNT(DISTINCT p.id) as articleCount,
               COALESCE(SUM(p.view_count), 0) as viewCount,
               COALESCE(SUM(p.like_count), 0) as likeCount,
               COALESCE(SUM(p.comment_count), 0) as commentCount
        FROM t_tags t
        JOIN t_post_tags pt ON t.id = pt.tag_id
        JOIN t_posts p ON pt.post_id = p.id AND p.status = 'PUBLISHED'
        GROUP BY t.id, t.name, date
        ORDER BY t.id, date ASC
    """)
    List<Map<String, Object>> getTagDailyMetrics();

    @Select("SELECT COUNT(*) FROM t_tags")
    long countAll();

    @Select("SELECT COUNT(*) FROM t_post_tags")
    long countAllPostTagRelations();
}
