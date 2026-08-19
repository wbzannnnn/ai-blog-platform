package com.lanou.springaidemo.service.impl;

import com.lanou.springaidemo.entity.Posts;
import com.lanou.springaidemo.entity.Tags;
import com.lanou.springaidemo.enums.Status;
import com.lanou.springaidemo.mapper.PostsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class SearchAgentService {

    private static final int MAX_ARTICLES = 8;
    private static final int MAX_HISTORY_TURNS = 8;
    private static final int MAX_CONTEXT_CHARS = 10_000;
    private static final Pattern QUOTED_TITLE = Pattern.compile("《([^》]{2,100})》");
    private static final Set<String> STOP_TERMS = Set.of(
            "一共", "共有", "总共", "多少", "几篇", "篇", "文章", "博客", "本站", "现有", "当前",
            "目前", "现在", "数量", "已经", "已", "有", "的", "吗", "呢",
            "请问", "请帮", "帮我", "一下", "介绍", "总结", "概括", "分析", "查找", "搜索",
            "关于", "相关", "内容", "主要", "核心", "观点", "讲了", "说了", "什么", "哪些",
            "怎么", "如何", "详细", "根据", "告诉", "列出", "分别", "最近", "最新", "发布",
            "在讲", "在写", "正在讲", "正在写"
    );

    private final PostsMapper postsMapper;
    private final ChatClient agentChatClient;

    private final Map<String, List<Map<String, String>>> conversations = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<Map<String, String>>> eldest) {
            return size() > 500;
        }
    };

    public SearchAgentService(PostsMapper postsMapper, ChatClient.Builder chatClientBuilder) {
        this.postsMapper = postsMapper;
        this.agentChatClient = chatClientBuilder.build();
    }

    public enum QueryIntent {
        COUNT,
        CATALOG,
        OVERVIEW,
        LATEST,
        SEARCH
    }

    public record ArticleSource(Long id, String title, String summary) {
    }

    public record ChatAnswer(String answer, String intent, List<ArticleSource> sources) {
    }

    public record AgentOverview(long publishedCount, List<ArticleSource> articles,
                                List<ArticleSource> recentArticles, List<String> recommendedQuestions) {
    }

    public ChatAnswer chat(String conversationId, String question) {
        String normalizedQuestion = question == null ? "" : question.trim();
        List<Posts> allPosts = postsMapper.findByStatusOrderByCreatedAtDesc(Status.PUBLISHED);
        QueryIntent intent = detectIntent(normalizedQuestion);
        List<Map<String, String>> history = getConversation(conversationId);

        if (allPosts.isEmpty()) {
            return remember(history, normalizedQuestion,
                    new ChatAnswer("目前还没有已发布文章，因此暂时没有可用于回答的站内内容。",
                            intentName(intent), List.of()));
        }

        if (intent == QueryIntent.COUNT) {
            return remember(history, normalizedQuestion, buildCountAnswer(normalizedQuestion, allPosts));
        }

        if (intent == QueryIntent.CATALOG) {
            List<Posts> catalog = limit(allPosts, 10);
            return remember(history, normalizedQuestion,
                    new ChatAnswer(buildCatalogAnswer(allPosts.size(), catalog), intentName(intent), toSources(catalog)));
        }

        List<Posts> relevantPosts = selectRelevantPosts(normalizedQuestion, allPosts, intent);
        if (relevantPosts.isEmpty()) {
            List<Posts> recentPosts = limit(allPosts, 3);
            String answer = buildNoMatchAnswer(normalizedQuestion, allPosts.size(), recentPosts);
            return remember(history, normalizedQuestion,
                    new ChatAnswer(answer, intentName(intent), toSources(recentPosts)));
        }

        String fallback = buildSummaryFallback(intent, allPosts.size(), relevantPosts);
        String knowledge = buildContext(relevantPosts, normalizedQuestion, intent);
        String historyText = buildHistory(history);
        String prompt = buildPrompt(normalizedQuestion, intent, knowledge, historyText, allPosts.size());

        String answer = fallback;
        try {
            log.info("SearchAgent: conv={}, intent={}, q={}, found={}",
                    conversationId, intent, normalizedQuestion, relevantPosts.size());
            String generated = agentChatClient.prompt()
                    .system("你是AI博客系统的内容检索助手。回答必须以提供的已发布文章为依据，不能补充站外知识或编造事实。")
                    .user(prompt)
                    .call()
                    .content();
            if (generated != null && !generated.isBlank()) {
                answer = generated.trim();
            }
        } catch (Exception e) {
            log.warn("SearchAgent generation failed, returning article-based fallback: {}", e.getMessage());
        }

        return remember(history, normalizedQuestion,
                new ChatAnswer(answer, intentName(intent), toSources(relevantPosts)));
    }

    public AgentOverview getOverview() {
        List<Posts> allPosts = postsMapper.findByStatusOrderByCreatedAtDesc(Status.PUBLISHED);
        return new AgentOverview(allPosts.size(), toSources(allPosts), toSources(limit(allPosts, 5)),
                getRecommendedQuestions(allPosts));
    }

    public List<String> getRecommendedQuestions() {
        return getRecommendedQuestions(postsMapper.findByStatusOrderByCreatedAtDesc(Status.PUBLISHED));
    }

    static QueryIntent detectIntent(String question) {
        String text = question == null ? "" : question.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
        boolean asksForCount = containsAny(text,
                "多少篇", "几篇", "文章数量", "文章总数", "一共有", "共有多少", "总共有");
        boolean asksWhatTheyCover = containsAny(text,
                "讲什么", "讲了什么", "在讲", "写什么", "写了什么", "在写", "主要内容", "分别讲", "分别写");
        if (asksForCount && asksWhatTheyCover) {
            return QueryIntent.CATALOG;
        }
        if (asksForCount) {
            return QueryIntent.COUNT;
        }
        if (containsAny(text, "所有文章", "文章列表", "有哪些文章", "列出文章", "标题和摘要", "全部文章")) {
            return QueryIntent.CATALOG;
        }
        if (containsAny(text, "最新文章", "最近发布", "近期文章", "最近在写")) {
            return QueryIntent.LATEST;
        }
        if (containsAny(text, "主要讲什么", "主要内容", "内容概览", "整体总结", "都在讨论", "关注什么", "主题分布")) {
            return QueryIntent.OVERVIEW;
        }
        return QueryIntent.SEARCH;
    }

    private ChatAnswer buildCountAnswer(String question, List<Posts> allPosts) {
        Set<String> terms = extractTerms(question);
        if (terms.isEmpty()) {
            String answer = "目前站内共有 **" + allPosts.size()
                    + " 篇已发布文章**。这个数字来自当前文章库，不包含草稿或未发布内容。";
            return new ChatAnswer(answer, intentName(QueryIntent.COUNT), List.of());
        }

        List<Posts> matchedPosts = scorePosts(question, allPosts);
        if (matchedPosts.isEmpty()) {
            String topic = String.join("、", terms);
            String answer = "当前 **" + allPosts.size() + " 篇已发布文章**中，没有找到明确讨论“"
                    + topic + "”的文章。";
            return new ChatAnswer(answer, intentName(QueryIntent.COUNT), List.of());
        }

        String topic = String.join("、", terms.stream().limit(3).toList());
        String answer = "当前 **" + allPosts.size() + " 篇已发布文章**中，有 **" + matchedPosts.size()
                + " 篇**与“" + topic + "”相关："
                + matchedPosts.stream().map(post -> "《" + post.getTitle() + "》").reduce((a, b) -> a + "、" + b).orElse("")
                + "。";
        return new ChatAnswer(answer, intentName(QueryIntent.COUNT), toSources(matchedPosts));
    }

    private String buildCatalogAnswer(int total, List<Posts> posts) {
        StringBuilder answer = new StringBuilder("目前共有 **")
                .append(total).append(" 篇已发布文章**。\n\n");
        for (int i = 0; i < posts.size(); i++) {
            Posts post = posts.get(i);
            answer.append(i + 1).append(". **《").append(post.getTitle()).append("》**\n")
                    .append("   ").append(summaryFor(post, 180)).append("\n");
        }
        if (total > posts.size()) {
            answer.append("\n以上先列出最近发布的 ").append(posts.size()).append(" 篇。");
        }
        return answer.toString().trim();
    }

    private String buildNoMatchAnswer(String question, int total, List<Posts> recentPosts) {
        String titles = recentPosts.stream()
                .map(post -> "《" + post.getTitle() + "》")
                .reduce((a, b) -> a + "、" + b)
                .orElse("暂无");
        return "我检查了当前 **" + total + " 篇已发布文章**，暂时没有足够内容回答“" + question
                + "”。现有内容包括 " + titles + " 等，可以换成其中的主题、标题或具体观点继续询问。";
    }

    private String buildSummaryFallback(QueryIntent intent, int total, List<Posts> posts) {
        String intro = switch (intent) {
            case OVERVIEW -> "根据当前 " + total + " 篇已发布文章，可以从以下内容理解这个博客：";
            case LATEST -> "最近发布的文章主要包括：";
            default -> "根据检索到的站内文章：";
        };
        StringBuilder answer = new StringBuilder(intro).append("\n\n");
        for (Posts post : posts) {
            answer.append("- **《").append(post.getTitle()).append("》**：")
                    .append(summaryFor(post, 220)).append("\n");
        }
        return answer.toString().trim();
    }

    private String buildPrompt(String question, QueryIntent intent, String knowledge,
                               String history, int totalPosts) {
        String task = switch (intent) {
            case OVERVIEW -> "归纳这些文章共同覆盖的主题和各自侧重点，不要逐篇机械复述。";
            case LATEST -> "概括最近文章分别讨论什么，并指出它们之间的内容联系。";
            default -> "直接回答问题，先给结论，再用文章中的信息解释。";
        };
        StringBuilder prompt = new StringBuilder()
                .append("当前文章库共有 ").append(totalPosts).append(" 篇已发布文章。\n")
                .append("任务：").append(task).append("\n")
                .append("要求：只依据下方文章资料；用《文章标题》标明依据；只有问题超出资料范围时才说明无法判断；避免宣传口号和固定客服话术。\n\n")
                .append("文章资料：\n").append(knowledge).append("\n");
        if (!history.isBlank()) {
            prompt.append("此前对话（仅用于理解指代，事实仍以文章资料为准）：\n")
                    .append(history).append("\n");
        }
        return prompt.append("用户问题：").append(question).toString();
    }

    private List<Posts> selectRelevantPosts(String question, List<Posts> allPosts, QueryIntent intent) {
        if (intent == QueryIntent.OVERVIEW) {
            return limit(allPosts, MAX_ARTICLES);
        }
        if (intent == QueryIntent.LATEST) {
            return limit(allPosts, 5);
        }
        return scorePosts(question, allPosts);
    }

    private List<Posts> scorePosts(String question, List<Posts> allPosts) {
        Set<String> terms = extractTerms(question);
        List<String> quotedTitles = extractQuotedTitles(question);
        if (terms.isEmpty() && quotedTitles.isEmpty()) {
            return List.of();
        }

        Map<Posts, Integer> scores = new LinkedHashMap<>();
        for (Posts post : allPosts) {
            String title = safe(post.getTitle()).toLowerCase(Locale.ROOT);
            String summary = safe(post.getSummary()).toLowerCase(Locale.ROOT);
            String content = safe(post.getContent()).toLowerCase(Locale.ROOT);
            String tags = loadTags(post).stream().map(Tags::getName)
                    .filter(name -> name != null)
                    .map(name -> name.toLowerCase(Locale.ROOT))
                    .reduce((a, b) -> a + " " + b).orElse("");
            int score = 0;

            for (String quotedTitle : quotedTitles) {
                String quoted = quotedTitle.toLowerCase(Locale.ROOT);
                if (title.contains(quoted) || quoted.contains(title)) score += 100;
            }
            for (String term : terms) {
                if (title.contains(term)) score += 12;
                if (tags.contains(term)) score += 8;
                if (summary.contains(term)) score += 4;
                if (content.contains(term)) score += 1;
            }
            if (score > 0) scores.put(post, score);
        }

        int maxScore = scores.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int relevanceThreshold = maxScore >= 8 ? Math.max(3, (int) Math.ceil(maxScore * 0.2)) : 1;
        return scores.entrySet().stream()
                .filter(entry -> entry.getValue() >= relevanceThreshold)
                .sorted(Map.Entry.<Posts, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(entry -> safeLong(entry.getKey().getCreatedAt()), Comparator.reverseOrder()))
                .limit(MAX_ARTICLES)
                .map(Map.Entry::getKey)
                .toList();
    }

    private Set<String> extractTerms(String question) {
        String normalized = safe(question).toLowerCase(Locale.ROOT);
        for (String stopTerm : STOP_TERMS) {
            normalized = normalized.replace(stopTerm, " ");
        }
        normalized = normalized.replaceAll("[\\p{Punct}，。！？、；：‘’“”《》（）【】\\s]+", " ").trim();
        Set<String> terms = new LinkedHashSet<>();
        for (String token : normalized.split("\\s+")) {
            if (token.length() < 2 || STOP_TERMS.contains(token)) continue;
            addTerm(terms, token);
            if (containsChinese(token) && token.length() > 4) {
                int maxLength = Math.min(4, token.length());
                for (int length = maxLength; length >= 2; length--) {
                    for (int start = 0; start <= token.length() - length; start++) {
                        addTerm(terms, token.substring(start, start + length));
                    }
                }
            }
        }
        return terms;
    }

    private void addTerm(Set<String> terms, String term) {
        String value = term.trim();
        if (value.length() >= 2 && !STOP_TERMS.contains(value)) terms.add(value);
    }

    private List<String> extractQuotedTitles(String question) {
        List<String> titles = new ArrayList<>();
        Matcher matcher = QUOTED_TITLE.matcher(safe(question));
        while (matcher.find()) titles.add(matcher.group(1).trim());
        return titles;
    }

    private String buildContext(List<Posts> posts, String question, QueryIntent intent) {
        Set<String> terms = extractTerms(question);
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < posts.size(); i++) {
            Posts post = posts.get(i);
            context.append("[文章 ").append(i + 1).append("]\n")
                    .append("标题：").append(post.getTitle()).append("\n");
            List<Tags> tags = loadTags(post);
            if (!tags.isEmpty()) {
                context.append("标签：")
                        .append(tags.stream().map(Tags::getName).filter(name -> name != null)
                                .reduce((a, b) -> a + "、" + b).orElse(""))
                        .append("\n");
            }
            context.append("摘要：").append(summaryFor(post, 500)).append("\n");
            if (intent == QueryIntent.SEARCH) {
                String excerpt = extractMatchingSentences(post.getContent(), terms);
                if (!excerpt.isBlank()) context.append("正文摘录：").append(excerpt).append("\n");
            }
            context.append("\n");
            if (context.length() >= MAX_CONTEXT_CHARS) break;
        }
        return context.length() > MAX_CONTEXT_CHARS
                ? context.substring(0, MAX_CONTEXT_CHARS)
                : context.toString();
    }

    private String extractMatchingSentences(String text, Set<String> terms) {
        String cleaned = cleanMarkdown(text);
        if (cleaned.isBlank()) return "";
        if (terms.isEmpty()) return truncate(cleaned, 600);

        List<String> matched = new ArrayList<>();
        for (String sentence : cleaned.split("[。！？\\n]+")) {
            String value = sentence.trim();
            if (value.length() < 8) continue;
            if (terms.stream().anyMatch(value.toLowerCase(Locale.ROOT)::contains)) {
                matched.add(truncate(value, 220));
            }
            if (matched.size() >= 4) break;
        }
        return String.join("。", matched);
    }

    private List<Tags> loadTags(Posts post) {
        if (post.getId() == null) return List.of();
        List<Tags> tags = postsMapper.selectTagsByPostId(post.getId());
        return tags == null ? List.of() : tags;
    }

    private List<String> getRecommendedQuestions(List<Posts> allPosts) {
        if (allPosts.isEmpty()) return List.of("目前有多少篇已发布文章？");
        List<String> questions = new ArrayList<>();
        questions.add("目前有多少篇已发布文章？");
        questions.add("根据现有文章总结这个博客主要关注什么");
        questions.add("最近发布的文章分别讲了什么？");
        questions.add("总结《" + allPosts.get(0).getTitle() + "》的核心观点");
        if (allPosts.size() > 1) {
            questions.add("比较《" + allPosts.get(0).getTitle() + "》和《"
                    + allPosts.get(1).getTitle() + "》的关注重点");
        }
        return questions;
    }

    private List<ArticleSource> toSources(List<Posts> posts) {
        return posts.stream()
                .map(post -> new ArticleSource(post.getId(), post.getTitle(), summaryFor(post, 150)))
                .toList();
    }

    private String summaryFor(Posts post, int maxLength) {
        String summary = cleanMarkdown(post.getSummary());
        if (summary.isBlank()) summary = cleanMarkdown(post.getContent());
        String title = cleanMarkdown(post.getTitle());
        if (!title.isBlank() && summary.startsWith(title)) {
            summary = summary.substring(title.length()).trim();
        }
        summary = summary.replaceFirst("^摘要\\s*[：:]\\s*", "").trim();
        if (summary.startsWith("以下是一段") && summary.contains("：")) {
            summary = summary.substring(summary.indexOf('：') + 1).trim();
        }
        summary = summary.replaceAll("[（(]?字数\\s*[：:]?\\s*\\d+[）)]?", "").trim();
        return summary.isBlank() ? "该文章暂未提供摘要。" : truncate(summary, maxLength);
    }

    private String cleanMarkdown(String value) {
        return safe(value)
                .replaceAll("!\\[[^]]*]\\([^)]*\\)", " ")
                .replaceAll("\\[([^]]+)]\\([^)]*\\)", "$1")
                .replaceAll("[`#>*_~]+", " ")
                .replaceAll("-{3,}", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength).trim() + "...";
    }

    private List<Posts> limit(List<Posts> posts, int maxSize) {
        return posts.size() <= maxSize ? List.copyOf(posts) : List.copyOf(posts.subList(0, maxSize));
    }

    private List<Map<String, String>> getConversation(String conversationId) {
        synchronized (conversations) {
            return conversations.computeIfAbsent(conversationId, ignored -> new ArrayList<>());
        }
    }

    private String buildHistory(List<Map<String, String>> history) {
        StringBuilder text = new StringBuilder();
        synchronized (history) {
            for (Map<String, String> turn : history) {
                text.append("用户：").append(turn.get("user")).append("\n")
                        .append("助手：").append(turn.get("assistant")).append("\n");
            }
        }
        return text.toString();
    }

    private ChatAnswer remember(List<Map<String, String>> history, String question, ChatAnswer answer) {
        synchronized (history) {
            history.add(Map.of("user", question, "assistant", answer.answer()));
            if (history.size() > MAX_HISTORY_TURNS) history.remove(0);
        }
        return answer;
    }

    private static boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) return true;
        }
        return false;
    }

    private boolean containsChinese(String value) {
        return value.codePoints().anyMatch(codePoint -> codePoint >= 0x4E00 && codePoint <= 0x9FFF);
    }

    private String intentName(QueryIntent intent) {
        return intent.name().toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private Long safeLong(Long value) {
        return value == null ? 0L : value;
    }
}
