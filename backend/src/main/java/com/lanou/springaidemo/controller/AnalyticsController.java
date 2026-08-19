package com.lanou.springaidemo.controller;

import com.lanou.springaidemo.dto.response.ApiResponse;
import com.lanou.springaidemo.exception.BusinessException;
import com.lanou.springaidemo.mapper.PostsMapper;
import com.lanou.springaidemo.mapper.TagsMapper;
import com.lanou.springaidemo.enums.Status;
import com.lanou.springaidemo.service.impl.TagAnalyticsService;
import com.lanou.springaidemo.service.impl.TagAnalyticsService.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;

/**
 * 数据分析接口
 *
 * 数据缺失说明：
 *   - 独立访客 (UV)：无埋点系统，暂不可用
 *   - 收藏数：无此数据表，暂不可用
 */
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final TagsMapper tagsMapper;
    private final PostsMapper postsMapper;
    private final TagAnalyticsService tagAnalyticsService;

    // ==================== 标签搜索 ====================

    @GetMapping("/tags/search")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchTags(
            @RequestParam(required = false, defaultValue = "") String keyword) {
        var tags = keyword.isBlank()
                ? tagsMapper.findTopByPostCount(50)
                : tagsMapper.findByNameContaining(keyword);
        var result = tags.stream().map(t -> Map.<String, Object>of(
                "id", t.getId(), "name", t.getName()
        )).toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ==================== 标签热度走势 ====================

    @GetMapping("/tag-trends")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTagTrends(
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false, defaultValue = "month") String granularity,
            @RequestParam(required = false) Long compareTagId) {

        // —— 参数校验 ——
        Instant start = parseDate(startDate, true);
        Instant end = parseDate(endDate, false);
        if (start.isAfter(end)) throw new BusinessException(400, "起始日期不能晚于结束日期");
        if (!Set.of("day", "week", "month").contains(granularity))
            throw new BusinessException(400, "granularity 仅支持 day/week/month");

        // —— 全局标签汇总 ——
        List<Map<String, Object>> tagStats = tagsMapper.getTagAnalytics();
        List<TagRawMetrics> rawMetrics = tagStats.stream().map(row ->
                new TagRawMetrics(
                        ((Number) row.get("tagId")).longValue(),
                        (String) row.get("tagName"),
                        ((Number) row.get("totalViews")).longValue(),
                        ((Number) row.get("totalLikes")).longValue(),
                        ((Number) row.get("totalComments")).longValue(),
                        ((Number) row.get("articleCount")).intValue()
                )).toList();

        List<TagHotResult> heatIndexes = tagAnalyticsService.computeHeatIndexes(rawMetrics);

        // —— 时间序列 ——
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("allTags", heatIndexes.stream().map(h -> Map.of(
                "tagId", h.tagId(), "tagName", h.tagName(),
                "heatIndex", h.heatIndex(), "score", h.heatIndex(),
                "views", h.views(), "likes", h.likes(), "comments", h.comments(),
                "articles", h.articles()
        )).toList());

        if (tagId != null) {
            // 当前周期
            List<TimeSeriesPoint> current = tagAnalyticsService.getTagTimeSeries(tagId, start, end, granularity);
            // 上一周期（环比对比）
            long duration = end.toEpochMilli() - start.toEpochMilli();
            Instant prevEnd = start;
            Instant prevStart = prevEnd.minus(duration, java.time.temporal.ChronoUnit.MILLIS);
            List<TimeSeriesPoint> previous = tagAnalyticsService.getTagTimeSeries(tagId, prevStart, prevEnd, granularity);

            Map<String, Object> trend = new LinkedHashMap<>();
            trend.put("tagId", tagId);
            trend.put("current", formatSeries(current));
            trend.put("previous", formatSeries(previous));

            // 汇总
            int curTotal = current.stream().mapToInt(p -> p.articleCount).sum();
            int prevTotal = previous.stream().mapToInt(p -> p.articleCount).sum();
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("curPeriodTotal", curTotal);
            summary.put("prevPeriodTotal", prevTotal);
            summary.put("changePercent", prevTotal > 0
                    ? Math.round((curTotal - prevTotal) * 1000.0 / prevTotal) / 10.0
                    : null);
            summary.put("peakLabel", current.stream().max(Comparator.comparingInt(p -> p.articleCount))
                    .map(p -> p.label).orElse("无"));
            trend.put("summary", summary);

            // 趋势分析文本
            trend.put("analysis", tagAnalyticsService.computeTrendAnalysis(current, previous));

            // 该标签综合热度指数；hotScore 暂时保留用于兼容旧前端。
            heatIndexes.stream().filter(h -> h.tagId().equals(tagId)).findFirst()
                    .ifPresent(h -> {
                        trend.put("heatIndex", h.heatIndex());
                        trend.put("hotScore", h.heatIndex());
                    });

            // 热门文章
            trend.put("topArticles", tagAnalyticsService.getTagTopArticles(tagId, 5));

            result.put("trend", trend);
        }

        // —— 对比标签 ——
        if (compareTagId != null) {
            List<TimeSeriesPoint> compareSeries = tagAnalyticsService.getTagTimeSeries(compareTagId, start, end, granularity);
            Map<String, Object> compare = new LinkedHashMap<>();
            compare.put("tagId", compareTagId);
            compare.put("current", formatSeries(compareSeries));
            int cTotal = compareSeries.stream().mapToInt(p -> p.articleCount).sum();
            compare.put("summary", Map.of("curPeriodTotal", cTotal));
            heatIndexes.stream().filter(h -> h.tagId().equals(compareTagId)).findFirst()
                    .ifPresent(h -> {
                        compare.put("heatIndex", h.heatIndex());
                        compare.put("hotScore", h.heatIndex());
                    });
            result.put("compare", compare);
        }

        result.put("lastUpdated", System.currentTimeMillis());
        result.put("dataNote", "综合热度指数 = 阅读量 + 点赞×3 + 评论×5 + 已发布文章×10，没有人为设置的满分值。互动量是文章当前累计值，走势图按文章发布日期归集；系统未保存互动事件时间，因此不表示逐日新增互动。独立访客(UV)和收藏数暂不可用。");

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ==================== 博客概览 ====================

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview() {
        long totalPosts = postsMapper.countByStatus(Status.PUBLISHED);
        long totalTags = tagsMapper.countAll();
        Map<String, Object> publishedMetrics = postsMapper.getPublishedMetrics();
        long totalViews = numberValue(publishedMetrics, "totalViews");
        long totalLikes = numberValue(publishedMetrics, "totalLikes");
        long totalComments = numberValue(publishedMetrics, "totalComments");
        long totalInteractions = totalLikes + totalComments;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalPosts", totalPosts);
        result.put("totalTags", totalTags);
        result.put("totalTagRelations", tagsMapper.countAllPostTagRelations());
        result.put("avgTagsPerPost", totalPosts > 0
                ? Math.round(tagsMapper.countAllPostTagRelations() * 10.0 / totalPosts) / 10.0 : 0);
        result.put("totalViews", totalViews);
        result.put("totalLikes", totalLikes);
        result.put("totalComments", totalComments);
        result.put("totalInteractions", totalInteractions);
        result.put("avgViewsPerPost", totalPosts > 0
                ? Math.round(totalViews * 10.0 / totalPosts) / 10.0 : 0);
        result.put("engagementRate", totalViews > 0
                ? Math.round(totalInteractions * 1000.0 / totalViews) / 10.0 : 0);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ==================== helpers ====================

    private List<Map<String, Object>> formatSeries(List<TimeSeriesPoint> points) {
        return points.stream().map(p -> Map.<String, Object>of(
                "label", p.label,
                "articleCount", p.articleCount,
                "viewCount", p.viewCount,
                "likeCount", p.likeCount,
                "commentCount", p.commentCount
        )).toList();
    }

    private long numberValue(Map<String, Object> values, String key) {
        Object value = values == null ? null : values.get(key);
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private Instant parseDate(String dateStr, boolean isStart) {
        if (dateStr == null || dateStr.isBlank()) {
            return isStart ? Instant.now().minus(90, java.time.temporal.ChronoUnit.DAYS) : Instant.now();
        }
        try {
            LocalDate ld = LocalDate.parse(dateStr);
            return (isStart ? ld.atStartOfDay(ZoneId.of("Asia/Shanghai"))
                    : ld.atTime(LocalTime.MAX).atZone(ZoneId.of("Asia/Shanghai"))).toInstant();
        } catch (Exception e) {
            throw new BusinessException(400, "日期格式错误，请使用 yyyy-MM-dd");
        }
    }
}
