package com.lanou.springaidemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lanou.springaidemo.entity.Posts;
import com.lanou.springaidemo.entity.Tags;
import com.lanou.springaidemo.enums.ModerationStatus;
import com.lanou.springaidemo.enums.Status;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author lanou
 * @since 2026-06-08
 */
@Mapper
public interface PostsMapper extends BaseMapper<Posts> {
    /**
     * 根据文章状态查询文章列表，按创建时间倒序排序
     * @param status 文章状态
     * @return 符合条件的文章集合
     */
    @Select("SELECT * FROM t_posts WHERE status = #{status} ORDER BY created_at DESC")
    List<Posts> findByStatusOrderByCreatedAtDesc(@Param("status") Status status);

    /**
     * 根据文章状态和作者ID查询文章列表，按创建时间倒序排序
     * @param status 文章状态
     * @param authorId 作者ID
     * @return 符合条件的文章集合
     */
    @Select("SELECT * FROM t_posts WHERE status = #{status} AND author_id = #{authorId} ORDER BY created_at DESC")
    List<Posts> findByStatusAndAuthorId(@Param("status") Status status, @Param("authorId") Long authorId);

    /**
     * 根据审核状态查询文章列表，按创建时间倒序排序
     * @param moderationStatus 审核状态
     * @return 符合条件的文章集合
     */
    @Select("SELECT * FROM t_posts WHERE moderation_status = #{moderationStatus} ORDER BY created_at DESC")
    List<Posts> findByModerationStatus(@Param("moderationStatus") ModerationStatus moderationStatus);

    /**
     * 根据状态+关键词搜索文章（标题/内容模糊查询）
     * @param status 文章状态
     * @param keyword 搜索关键词
     * @return 符合条件的文章集合
     */
    @Select("SELECT * FROM t_posts WHERE status = #{status} AND (title LIKE CONCAT('%', #{keyword}, '%') OR content LIKE CONCAT('%', #{keyword}, '%')) ORDER BY created_at DESC")
    List<Posts> searchByKeyword(@Param("status") Status status, @Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM t_posts WHERE status = #{status} AND (title LIKE CONCAT('%', #{keyword}, '%') OR content LIKE CONCAT('%', #{keyword}, '%'))")
    long countByKeyword(@Param("status") Status status, @Param("keyword") String keyword);

    /**
     * 根据文章状态+标签名称查询关联文章
     * @param status 文章状态
     * @param tagName 标签名称
     * @return 符合条件的文章集合
     */
    @Select("SELECT p.* FROM t_posts p JOIN post_tags pt ON p.id = pt.post_id JOIN tags t ON pt.tag_id = t.id WHERE p.status = #{status} AND t.name = #{tagName} ORDER BY p.created_at DESC")
    List<Posts> findByTagName(@Param("status") Status status, @Param("tagName") String tagName);

    /**
     * 根据文章状态+作者用户名查询文章
     * @param status 文章状态
     * @param username 作者用户名
     * @return 符合条件的文章集合
     */
    @Select("SELECT p.* FROM t_posts p JOIN users u ON p.author_id = u.id WHERE p.status = #{status} AND u.username = #{username} ORDER BY p.created_at DESC")
    List<Posts> findByAuthorUsername(@Param("status") Status status, @Param("username") String username);

    /**
     * 统计指定状态的文章总数
     * @param status 文章状态
     * @return 文章数量
     */
    @Select("SELECT COUNT(*) FROM t_posts WHERE status = #{status}")
    long countByStatus(@Param("status") Status status);

    /**
     * 查询指定状态下浏览量最高的前10篇文章
     * @param status 文章状态
     * @return 热门文章列表
     */
    @Select("SELECT * FROM t_posts WHERE status = #{status} ORDER BY view_count DESC LIMIT 10")
    List<Posts> findTop10ByStatusOrderByViewCountDesc(@Param("status") Status status);

    /**
     * 查询全站已发布内容的核心互动指标。
     */
    @Select("""
        SELECT COALESCE(SUM(view_count), 0) as totalViews,
               COALESCE(SUM(like_count), 0) as totalLikes,
               COALESCE(SUM(comment_count), 0) as totalComments
        FROM t_posts
        WHERE status = 'PUBLISHED'
    """)
    Map<String, Object> getPublishedMetrics();

    /**
     * 按与标签热度相同的互动权重查询标签下热门文章。
     */
    @Select("""
        SELECT p.*
        FROM t_posts p
        JOIN t_post_tags pt ON p.id = pt.post_id
        WHERE pt.tag_id = #{tagId} AND p.status = #{status}
        ORDER BY (p.view_count + p.like_count * 3 + p.comment_count * 5) DESC,
                 p.created_at DESC
        LIMIT #{limit}
    """)
    List<Posts> findTopByTagOrderByHeat(@Param("tagId") Long tagId,
                                        @Param("status") Status status,
                                        @Param("limit") int limit);

    /**
     * 查询指定状态下最新发布的前10篇文章
     * @param status 文章状态
     * @return 最新文章列表
     */
    @Select("SELECT * FROM t_posts WHERE status = #{status} ORDER BY created_at DESC LIMIT 10")
    List<Posts> findTop10ByStatusOrderByCreatedAtDesc(@Param("status") Status status);

    /**
     * 查询最新发布的指定数量文章
     * @param limit 查询条数
     * @return 最新文章列表
     */
    @Select("SELECT * FROM t_posts ORDER BY created_at DESC LIMIT #{limit}")
    List<Posts> findTopByOrderByCreatedAtDesc(@Param("limit") int limit);

    /**
     * 根据文章ID查询关联的所有标签
     * @param postId 文章ID
     * @return 标签列表
     */
    @Select("SELECT t.* FROM t_tags t JOIN t_post_tags pt ON t.id = pt.tag_id WHERE pt.post_id = #{postId}")
    List<Tags> selectTagsByPostId(@Param("postId") Long postId);

    /**
     * 新增文章与标签的关联关系
     * @param postId 文章ID
     * @param tagId 标签ID
     */
    @Insert("INSERT INTO t_post_tags (post_id, tag_id) VALUES (#{postId}, #{tagId})")
    void insertPostTag(@Param("postId") Long postId, @Param("tagId") Long tagId);

    /**
     * 根据文章ID删除该文章所有标签关联关系
     * @param postId 文章ID
     */
    @Delete("DELETE FROM t_post_tags WHERE post_id = #{postId}")
    void deletePostTagsByPostId(@Param("postId") Long postId);
}
