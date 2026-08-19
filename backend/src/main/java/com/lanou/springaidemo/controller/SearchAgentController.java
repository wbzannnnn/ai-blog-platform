package com.lanou.springaidemo.controller;

import com.lanou.springaidemo.dto.response.ApiResponse;
import com.lanou.springaidemo.service.impl.SearchAgentService;
import com.lanou.springaidemo.service.impl.SearchAgentService.AgentOverview;
import com.lanou.springaidemo.service.impl.SearchAgentService.ChatAnswer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class SearchAgentController {

    private final SearchAgentService searchAgentService;

    /**
     * 智能搜索对话接口
     * POST /agent/chat
     * { "question": "...", "conversationId": "xxx" (可选，首次为空则自动创建) }
     */
    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chat(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        if (question == null || question.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .code(400).message("问题不能为空").data(null).timestamp(System.currentTimeMillis()).build());
        }

        String conversationId = body.get("conversationId");
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString().substring(0, 8);
        }

        ChatAnswer chatAnswer = searchAgentService.chat(conversationId, question);

        Map<String, Object> result = Map.of(
                "conversationId", conversationId,
                "question", question,
                "answer", chatAnswer.answer(),
                "intent", chatAnswer.intent(),
                "sources", chatAnswer.sources()
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取推荐问题
     * GET /agent/recommended
     */
    @GetMapping("/recommended")
    public ResponseEntity<ApiResponse<List<String>>> getRecommendedQuestions() {
        return ResponseEntity.ok(ApiResponse.success(searchAgentService.getRecommendedQuestions()));
    }

    /**
     * 返回由当前已发布文章生成的检索页概览。
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AgentOverview>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(searchAgentService.getOverview()));
    }
}
