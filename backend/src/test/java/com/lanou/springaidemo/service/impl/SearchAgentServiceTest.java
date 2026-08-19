package com.lanou.springaidemo.service.impl;

import com.lanou.springaidemo.entity.Posts;
import com.lanou.springaidemo.enums.Status;
import com.lanou.springaidemo.mapper.PostsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchAgentServiceTest {

    private PostsMapper postsMapper;
    private SearchAgentService service;

    @BeforeEach
    void setUp() {
        postsMapper = mock(PostsMapper.class);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(mock(ChatClient.class));
        service = new SearchAgentService(postsMapper, builder);
    }

    @Test
    void detectIntentRecognizesDataAndSummaryQuestions() {
        assertEquals(SearchAgentService.QueryIntent.COUNT,
                SearchAgentService.detectIntent("一共有几篇文章？"));
        assertEquals(SearchAgentService.QueryIntent.CATALOG,
                SearchAgentService.detectIntent("现在有几篇文章在讲什么"));
        assertEquals(SearchAgentService.QueryIntent.CATALOG,
                SearchAgentService.detectIntent("列出所有文章的标题和摘要"));
        assertEquals(SearchAgentService.QueryIntent.OVERVIEW,
                SearchAgentService.detectIntent("这个博客主要讲什么？"));
        assertEquals(SearchAgentService.QueryIntent.LATEST,
                SearchAgentService.detectIntent("最近发布的文章分别讲了什么？"));
    }

    @Test
    void countQuestionReturnsPublishedArticleCountWithoutCallingAi() {
        when(postsMapper.findByStatusOrderByCreatedAtDesc(Status.PUBLISHED))
                .thenReturn(List.of(post(3L, "第三篇", "摘要三"), post(2L, "第二篇", "摘要二"),
                        post(1L, "第一篇", "摘要一")));

        SearchAgentService.ChatAnswer result = service.chat("count-test", "一共有几篇文章？");

        assertEquals("count", result.intent());
        assertTrue(result.answer().contains("3 篇已发布文章"));
        assertTrue(result.sources().isEmpty());
    }

    @Test
    void naturalPublishedCountQuestionDoesNotTreatGenericWordsAsTopic() {
        when(postsMapper.findByStatusOrderByCreatedAtDesc(Status.PUBLISHED))
                .thenReturn(List.of(post(2L, "第二篇", "摘要二"), post(1L, "第一篇", "摘要一")));

        SearchAgentService.ChatAnswer result = service.chat("natural-count-test", "目前有多少篇已发布文章？");

        assertEquals("count", result.intent());
        assertTrue(result.answer().contains("2 篇已发布文章"));
        assertTrue(result.answer().contains("这个数字来自当前文章库"));
    }

    @Test
    void combinedCountAndContentQuestionReturnsArticleCatalog() {
        when(postsMapper.findByStatusOrderByCreatedAtDesc(Status.PUBLISHED))
                .thenReturn(List.of(post(2L, "Java企业开发", "讨论Java生态与稳定性。"),
                        post(1L, "AI行业应用", "讨论AI在制造和医疗中的应用。")));

        SearchAgentService.ChatAnswer result = service.chat("combined-test", "现在有几篇文章在讲什么");

        assertEquals("catalog", result.intent());
        assertTrue(result.answer().contains("2 篇已发布文章"));
        assertTrue(result.answer().contains("Java企业开发"));
        assertTrue(result.answer().contains("讨论AI在制造和医疗中的应用"));
        assertEquals(2, result.sources().size());
    }

    @Test
    void topicCountStillCountsMatchingArticles() {
        Posts javaPost = post(2L, "Java企业开发", "讨论Java生态与稳定性。");
        Posts aiPost = post(1L, "AI行业应用", "讨论AI在制造业中的应用。");
        when(postsMapper.findByStatusOrderByCreatedAtDesc(Status.PUBLISHED))
                .thenReturn(List.of(javaPost, aiPost));
        when(postsMapper.selectTagsByPostId(2L)).thenReturn(List.of());
        when(postsMapper.selectTagsByPostId(1L)).thenReturn(List.of());

        SearchAgentService.ChatAnswer result = service.chat("topic-count-test", "目前有几篇Java文章？");

        assertEquals("count", result.intent());
        assertTrue(result.answer().contains("有 **1 篇**"));
        assertEquals("Java企业开发", result.sources().get(0).title());
    }

    @Test
    void catalogQuestionUsesStoredTitlesAndSummaries() {
        when(postsMapper.findByStatusOrderByCreatedAtDesc(Status.PUBLISHED))
                .thenReturn(List.of(post(2L, "Java在企业开发中的位置", "讨论Java的稳定性与生态。"),
                        post(1L, "Vibe Coding的意义", "讨论情感计算与用户体验。")));

        SearchAgentService.ChatAnswer result = service.chat("catalog-test", "列出所有文章的标题和摘要");

        assertEquals("catalog", result.intent());
        assertTrue(result.answer().contains("Java在企业开发中的位置"));
        assertTrue(result.answer().contains("讨论Java的稳定性与生态"));
        assertEquals(2, result.sources().size());
    }

    @Test
    void overviewIsBuiltFromCurrentArticles() {
        when(postsMapper.findByStatusOrderByCreatedAtDesc(Status.PUBLISHED))
                .thenReturn(List.of(post(1L, "AI行业应用", "讨论AI对制造、医疗和金融的影响。")));

        SearchAgentService.AgentOverview result = service.getOverview();

        assertEquals(1, result.publishedCount());
        assertEquals(1, result.articles().size());
        assertEquals("AI行业应用", result.recentArticles().get(0).title());
        assertTrue(result.recommendedQuestions().stream().anyMatch(question -> question.contains("AI行业应用")));
    }

    @Test
    void overviewReturnsCompleteIndexBeyondFiveArticles() {
        when(postsMapper.findByStatusOrderByCreatedAtDesc(Status.PUBLISHED))
                .thenReturn(List.of(
                        post(7L, "第七篇", "摘要七"), post(6L, "第六篇", "摘要六"),
                        post(5L, "第五篇", "摘要五"), post(4L, "第四篇", "摘要四"),
                        post(3L, "第三篇", "摘要三"), post(2L, "第二篇", "摘要二"),
                        post(1L, "第一篇", "摘要一")));

        SearchAgentService.AgentOverview result = service.getOverview();

        assertEquals(7, result.publishedCount());
        assertEquals(7, result.articles().size());
        assertEquals("第七篇", result.articles().get(0).title());
        assertEquals(5, result.recentArticles().size());
    }

    @Test
    void overviewRemovesGeneratedSummaryWrapperWithoutChangingStoredPost() {
        Posts post = post(1L, "AI行业应用",
                "以下是一段严格控制字数的摘要，适用于首页导语：AI正在改变制造和医疗流程。（字数：18）");
        when(postsMapper.findByStatusOrderByCreatedAtDesc(Status.PUBLISHED)).thenReturn(List.of(post));

        SearchAgentService.AgentOverview result = service.getOverview();

        assertEquals("AI正在改变制造和医疗流程。", result.recentArticles().get(0).summary());
        assertTrue(post.getSummary().startsWith("以下是一段"));
    }

    @Test
    void topicSearchDropsWeakIncidentalMatchesFromSources() {
        Posts javaPost = post(2L, "Java开发的优势与局限", "Java具备跨平台优势，也存在内存占用问题。");
        Posts unrelatedPost = post(1L, "团队协作方法", "文中顺带提到工具优势，但主题是协作流程。");
        when(postsMapper.findByStatusOrderByCreatedAtDesc(Status.PUBLISHED))
                .thenReturn(List.of(javaPost, unrelatedPost));
        when(postsMapper.selectTagsByPostId(2L)).thenReturn(List.of());
        when(postsMapper.selectTagsByPostId(1L)).thenReturn(List.of());

        SearchAgentService.ChatAnswer result = service.chat("search-test", "Java的优势和局限是什么？");

        assertEquals(1, result.sources().size());
        assertEquals("Java开发的优势与局限", result.sources().get(0).title());
    }

    private Posts post(Long id, String title, String summary) {
        return Posts.builder()
                .id(id)
                .title(title)
                .summary(summary)
                .content(summary)
                .status(Status.PUBLISHED)
                .createdAt(id)
                .build();
    }
}
