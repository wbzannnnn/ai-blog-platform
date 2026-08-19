package com.lanou.springaidemo.controller;

import com.lanou.springaidemo.dto.request.AiGenerateRequest;
import com.lanou.springaidemo.dto.response.ApiResponse;
import com.lanou.springaidemo.entity.Users;
import com.lanou.springaidemo.service.impl.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 功能控制器
 * 提供文章生成、摘要生成、标签生成等 AI 相关接口
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    // AI 核心服务
    private final AiService aiService;

    /**
     * AI 一键生成文章（正文 + 摘要 + 标签）
     * 权限：USER / ADMIN 均可访问
     * @param request 生成请求（主题、字数）
     * @param user 当前登录用户
     * @return 生成结果：文章内容、摘要、标签
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')") // 权限控制
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateArticle(
            @Valid @RequestBody AiGenerateRequest request,  // 接收并校验请求体
            @AuthenticationPrincipal Users user) {           // 获取当前登录用户

        // 1. 调用 AI 生成文章正文
        String content = aiService.generateArticle(request);
        // 2. 调用 AI 生成文章摘要
        String summary = aiService.generateSummary(content);
        // 3. 调用 AI 生成文章标签
        List<String> tags = aiService.generateTags(content);

        // 封装返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("content", content);
        result.put("summary", summary);
        result.put("tags", tags);

        // 成功返回
        return ResponseEntity.ok(ApiResponse.success("AI生成完成", result));
    }

    /**
     * 仅生成文章摘要
     * 权限：USER / ADMIN
     * @param request 包含文章内容 content
     * @return 摘要字符串
     */
    @PostMapping("/summarize")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> generateSummary(@RequestBody Map<String, String> request) {
        // 获取文章内容
        String content = request.get("content");
        // 生成摘要
        String summary = aiService.generateSummary(content);
        // 返回结果
        return ResponseEntity.ok(ApiResponse.success("摘要生成成功", summary));
    }

    /**
     * 仅生成文章标签
     * 权限：USER / ADMIN
     * @param request 包含文章内容 content
     * @return 标签列表
     */
    @PostMapping("/tags")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> generateTags(@RequestBody Map<String, String> request) {
        // 获取文章内容
        String content = request.get("content");
        // 生成标签
        List<String> tags = aiService.generateTags(content);
        // 返回结果
        return ResponseEntity.ok(ApiResponse.success("标签生成成功", tags));
    }
}