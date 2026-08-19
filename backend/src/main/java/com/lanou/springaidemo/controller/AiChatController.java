package com.lanou.springaidemo.controller;

import com.lanou.springaidemo.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI 智能助手控制器
 * 提供基于 Tool Calling 的智能对话功能
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class AiChatController {

    private final ChatClient chatClient;

    /**
     * 通用聊天接口
     * 支持自然语言与博客系统交互
     */
    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, String> request, Authentication authentication) {
        String userMessage = request.get("message");
        
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new BusinessException(400, "消息内容不能为空");
        }
        
        log.info("AI聊天请求 - 用户: {}, 消息: {}", authentication.getName(), userMessage);
        
        try {
            String response = chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .content();
            
            log.info("AI聊天响应 - 消息长度: {}", response.length());
            
            return ResponseEntity.ok(Map.of(
                    "code", 200,
                    "message", "success",
                    "data", Map.of(
                            "response", response,
                            "timestamp", System.currentTimeMillis()
                    )
            ));
        } catch (Exception e) {
            log.error("AI聊天失败", e);
            throw new BusinessException(500, "AI服务调用失败: " + e.getMessage());
        }
    }

    /**
     * 获取 AI 助手可用功能列表
     */
    @GetMapping("/capabilities")
    public ResponseEntity<?> getCapabilities() {
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "success",
                "data", Map.of(
                        "name", "博客智能助手",
                        "description", "基于 AI 的博客管理系统助手，可以帮你查询数据、管理文章、搜索内容等",
                        "capabilities", Map.of(
                                "dataQuery", Map.of(
                                        "name", "数据查询",
                                        "description", "查询系统统计数据、文章列表、用户信息等",
                                        "tools", new String[]{
                                                "get_statistics - 获取系统统计数据",
                                                "get_posts_list - 获取文章列表",
                                                "get_post_detail - 获取文章详情",
                                                "get_users_list - 获取用户列表",
                                                "get_user_detail - 获取用户详情",
                                                "get_hot_posts - 获取热门文章",
                                                "get_recent_posts - 获取最近文章"
                                        }
                                ),
                                "postManagement", Map.of(
                                        "name", "文章管理",
                                        "description", "审核文章、修改状态、删除文章等",
                                        "tools", new String[]{
                                                "approve_post - 通过文章审核",
                                                "reject_post - 拒绝文章",
                                                "delete_post - 删除文章",
                                                "update_post_status - 更新文章状态",
                                                "get_pending_posts - 获取待审核文章"
                                        }
                                ),
                                "userManagement", Map.of(
                                        "name", "用户管理",
                                        "description", "管理用户角色、禁用/启用账户等",
                                        "tools", new String[]{
                                                "update_user_role - 更新用户角色",
                                                "disable_user - 禁用用户",
                                                "enable_user - 启用用户",
                                                "get_admins - 获取管理员列表",
                                                "search_users - 搜索用户"
                                        }
                                ),
                                "search", Map.of(
                                        "name", "智能搜索",
                                        "description", "搜索文章、标签、作者等",
                                        "tools", new String[]{
                                                "search_posts - 搜索文章",
                                                "search_posts_by_tag - 按标签搜索",
                                                "search_posts_by_author - 按作者搜索",
                                                "advanced_search - 高级搜索",
                                                "get_all_tags - 获取所有标签",
                                                "get_popular_tags - 获取热门标签",
                                                "get_related_posts - 获取相关文章"
                                        }
                                )
                        ),
                        "exampleQuestions", new String[]{
                                "系统今天有多少篇文章？",
                                "帮我看看有哪些待审核的文章",
                                "搜索关于 Spring Boot 的文章",
                                "最近最热门的文章有哪些？",
                                "有哪些使用 Vue3 的教程？",
                                "把 ID 为 5 的文章审核通过",
                                "查看用户 admin 的文章列表",
                                "给我推荐几篇 React 相关的文章"
                        }
                )
        ));
    }
}
