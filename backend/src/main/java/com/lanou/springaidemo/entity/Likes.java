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
@TableName("t_likes")
@Schema(name = "Likes", description = "")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Likes implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long postId;

    private Long commentId;

    @Builder.Default
    private Long createdAt = System.currentTimeMillis();
}
