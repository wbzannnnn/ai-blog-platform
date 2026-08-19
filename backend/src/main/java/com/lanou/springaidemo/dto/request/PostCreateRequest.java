package com.lanou.springaidemo.dto.request;

import com.lanou.springaidemo.enums.Status;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostCreateRequest {
    @NotBlank(message = "标题不能为空")
    private String title;
    
    @NotBlank(message = "内容不能为空")
    private String content;
    
    private String summary;           // 文章摘要
    private List<String> tags;        // 标签列表
    private Boolean isAiGenerated;    // 是否AI生成
    private Status status;            // 发布状态
}