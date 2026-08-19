package com.lanou.springaidemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lanou.springaidemo.enums.ModerationStatus;
import com.lanou.springaidemo.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.ibatis.type.EnumTypeHandler;

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
@TableName("t_posts")
@Schema(name = "Posts", description = "")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Posts implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private String summary;

    @Builder.Default
    private Boolean isAiGenerated = false;

    private Long authorId;

    @Builder.Default
    private Integer likeCount = 0;

    @Builder.Default
    private Integer commentCount = 0;

    @Builder.Default
    private Integer viewCount = 0;

    @TableField(typeHandler = EnumTypeHandler.class)
    @Builder.Default
    private Status status = Status.DRAFT;

    @Builder.Default
    @TableField(typeHandler = EnumTypeHandler.class)
    private ModerationStatus moderationStatus = ModerationStatus.PENDING;

    private String moderationResult;

    @Builder.Default
    private Long createdAt = System.currentTimeMillis();

    @Builder.Default
    private Long updatedAt = System.currentTimeMillis();
}
