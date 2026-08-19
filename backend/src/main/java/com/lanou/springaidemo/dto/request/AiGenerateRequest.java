package com.lanou.springaidemo.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiGenerateRequest {
    @NotBlank(message = "主题不能为空")
    private String topic;           // 文章主题
    
    @Min(value = 100, message = "文章长度至少100字")
    @Max(value = 3000, message = "文章长度不能超过3000字")
    private Integer length = 500;   // 文章长度（默认500字）
}