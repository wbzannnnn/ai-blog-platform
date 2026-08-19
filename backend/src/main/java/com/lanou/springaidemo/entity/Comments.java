package com.lanou.springaidemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @author lanou
 * @since 2026-06-08
 */
@Data
@TableName("t_comments")
@Schema(name = "Comments", description = "")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Comments implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String content;

    private Long postId;

    private Long authorId;

    private Long parentId;

    @Builder.Default
    private Integer likeCount = 0;

    @Builder.Default
    private Long createdAt = System.currentTimeMillis();

    @Builder.Default
    private Long updatedAt = System.currentTimeMillis();
}
