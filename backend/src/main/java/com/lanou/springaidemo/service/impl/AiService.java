package com.lanou.springaidemo.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanou.springaidemo.dto.request.AiGenerateRequest;
import com.lanou.springaidemo.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private final ChatClient chatClient;

    public String generateArticle(AiGenerateRequest request) {
        String systemPrompt = """
                你是一个专业的博客文章撰写助手。请根据用户提供的主题，撰写一篇高质量、有深度的博客文章。
                要求：
                1. 文章结构清晰，有引言、正文和结论
                2. 内容详实，提供具体的例子和数据支持
                3. 语言流畅，逻辑严谨
                4. 长度适中，约{length}字左右
                5. 使用markdown格式排版
                """;

        String userPrompt = "请撰写一篇关于「" + request.getTopic() + "」的博客文章";

        try {
            log.info("开始调用AI生成文章，主题: {}", request.getTopic());
            String content = chatClient
                    .prompt()
                    .system(systemPrompt.replace("{length}", String.valueOf(request.getLength())))
                    .user(userPrompt)
                    .call()
                    .content();
            log.info("AI文章生成成功，主题: {}, 内容长度: {}", request.getTopic(), content.length());
            return content;
        } catch (ResourceAccessException e) {
            log.error("AI文章生成超时，主题: {}", request.getTopic(), e);
            throw new BusinessException(504, "AI文章生成超时，请稍后重试或检查网络连接");
        } catch (Exception e) {
            log.error("AI文章生成失败，主题: {}", request.getTopic(), e);
            throw new BusinessException(500, "AI文章生成失败: " + e.getMessage());
        }
    }

    public List<String> generateTags(String content) {
        String systemPrompt = """
                你是一个标签生成助手。请根据提供的文章内容，生成5-8个最相关的标签。
                要求：
                1. 标签要准确反映文章主题
                2. 使用中文标签
                3. **只返回纯JSON数组，不要加任何解释、不要加```json、不要加```、不要加文字、不要加多余符号**
                4. 例子：["Java","Spring Boot","后端"]
                记住：只输出JSON，不要输出任何其他内容！
                """;

        try {
            String result = chatClient
                    .prompt()
                    .system(systemPrompt)
                    .user(content)
                    .call()
                    .content();
            log.info("AI标签生成成功");
            return parseTagsFromJson(result);
        } catch (Exception e) {
            log.error("AI标签生成失败", e);
            throw new BusinessException(500, "AI标签生成失败: " + e.getMessage());
        }
    }

    public String generateSummary(String content) {
        String systemPrompt = """
                你是一个文章摘要生成助手。请根据提供的文章内容，生成一段简洁的摘要。
                要求：
                1. 摘要长度在100-200字之间
                2. 涵盖文章主要内容和核心观点
                3. 语言简洁明了
                """;

        try {
            String summary = chatClient
                    .prompt()
                    .system(systemPrompt)
                    .user(content)
                    .call()
                    .content();
            log.info("AI摘要生成成功");
            return summary;
        } catch (Exception e) {
            log.error("AI摘要生成失败", e);
            throw new BusinessException(500, "AI摘要生成失败: " + e.getMessage());
        }
    }

    public ModerationResult moderateContent(String content) {
        String systemPrompt = """
                你是一个内容审核助手。请根据以下规则审核内容：
                规则：
                1. 禁止涉及政治敏感内容
                2. 禁止涉及色情、低俗内容
                3. 禁止涉及暴力、恐怖内容
                4. 禁止涉及违法、违规内容
                5. 禁止涉及恶意攻击、侮辱他人的内容
                
                请判断内容是否符合规范，并给出审核结果和原因。
                返回格式：{"approved": true/false, "reason": "审核原因"}
                """;

        try {
            String result = chatClient
                    .prompt()
                    .system(systemPrompt)
                    .user(content)
                    .call()
                    .content();
            log.info("AI内容审核完成");
            return parseModerationResult(result);
        } catch (Exception e) {
            log.error("AI内容审核失败", e);
            return new ModerationResult(false, "审核服务异常");
        }
    }

    private List<String> parseTagsFromJson(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析标签JSON失败，返回默认标签", e);
            return List.of("未分类");
        }
    }

    private ModerationResult parseModerationResult(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, ModerationResult.class);
        } catch (Exception e) {
            log.warn("解析审核结果JSON失败", e);
            return new ModerationResult(true, "审核通过");
        }
    }

    public record ModerationResult(Boolean approved, String reason) {}
}