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
@TableName("t_tags")
@Schema(name = "Tags", description = "")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Tags implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    @Builder.Default
    private Long createdAt = System.currentTimeMillis();
}
