package com.lanou.springaidemo.service.impl;

import com.lanou.springaidemo.enums.Status;
import com.lanou.springaidemo.mapper.CommentsMapper;
import com.lanou.springaidemo.mapper.LikesMapper;
import com.lanou.springaidemo.mapper.PostsMapper;
import com.lanou.springaidemo.mapper.TagsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 标签热度分析服务 —— 所有热度计算逻辑集中在此，不散落在 UI
 *
 * 热度指数：
 *   综合热度 = 浏览量 + 点赞×3 + 评论×5 + 已发布文章数×10
 *   指数由真实互动数据直接累加，用于排序和横向比较，不设置虚构的满分值。
 *
 * 数据缺失说明：
 *   - 独立访客 (UV)：当前无埋点系统，不可用
 *   - 收藏数：无此表，不可用
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TagAnalyticsService {

    private final TagsMapper tagsMapper;
    private final PostsMapper postsMapper;
    private final CommentsMapper commentsMapper;
    private final LikesMapper likesMapper;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter WEEK_FMT = DateTimeFormatter.ofPattern("YYYY-'W'ww");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ==================== 核心热度计算 ====================

    /**
     * 对一批标签原始指标计算可累加的综合热度指数。
     */
    public List<TagHotResult> computeHeatIndexes(List<TagRawMetrics> rawList) {
        if (rawList.isEmpty()) return List.of();

        return rawList.stream().map(r -> {
            long heatIndex = Math.max(0,
                    r.views + r.likes * 3L + r.comments * 5L + r.articles * 10L);
            return new TagHotResult(r.tagId, r.tagName, heatIndex,
                    r.views, r.likes, r.comments, r.articles);
        }).sorted((a, b) -> Long.compare(b.heatIndex, a.heatIndex)).collect(Collectors.toList());
    }

    // ==================== 时间序列聚合 ====================

    public List<TimeSeriesPoint> getTagTimeSeries(
            Long tagId, Instant startDate, Instant endDate, String granularity) {

        List<Map<String, Object>> raw = tagsMapper.getTagDailyMetrics();
        if (raw.isEmpty()) return List.of();

        LocalDate start = LocalDate.ofInstant(startDate, ZoneId.of("Asia/Shanghai"));
        LocalDate end = LocalDate.ofInstant(endDate, ZoneId.of("Asia/Shanghai"));

        // 按粒度分组
        Map<String, TimeSeriesPoint> grouped = new LinkedHashMap<>();

        for (Map<String, Object> row : raw) {
            Long rowTagId = ((Number) row.get("tagId")).longValue();
            if (!rowTagId.equals(tagId)) continue;

            LocalDate date = LocalDate.parse((String) row.get("date"), DAY_FMT);
            if (date.isBefore(start) || date.isAfter(end)) continue;
            String bucket = resolveBucket(date, granularity);

            int count = ((Number) row.get("articleCount")).intValue();
            int views = ((Number) row.get("viewCount")).intValue();
            int likes = ((Number) row.get("likeCount")).intValue();
            int comments = ((Number) row.get("commentCount")).intValue();

            grouped.compute(bucket, (k, v) -> {
                if (v == null) return new TimeSeriesPoint(k, count, views, likes, comments);
                return new TimeSeriesPoint(k,
                        v.articleCount + count,
                        v.viewCount + views,
                        v.likeCount + likes,
                        v.commentCount + comments);
            });
        }

        return new ArrayList<>(grouped.values());
    }

    // ==================== 热门文章 ====================

    public List<Map<String, Object>> getTagTopArticles(Long tagId, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        var hotPosts = postsMapper.findTopByTagOrderByHeat(tagId, Status.PUBLISHED, limit);
        for (var post : hotPosts) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", post.getId());
            item.put("title", post.getTitle());
            item.put("viewCount", post.getViewCount());
            item.put("likeCount", post.getLikeCount());
            item.put("commentCount", post.getCommentCount());
            item.put("createdAt", post.getCreatedAt());
            results.add(item);
        }
        return results;
    }

    // ==================== 环比计算 ====================

    public String computeTrendAnalysis(List<TimeSeriesPoint> current, List<TimeSeriesPoint> previous) {
        if (current.isEmpty() && previous.isEmpty())
            return "当前数据不足，暂时无法判断趋势。";

        if (current.isEmpty())
            return "该标签在本周期暂无新发布文章，无法判断趋势。";

        if (previous.isEmpty())
            return "暂无上一周期数据用于对比。";

        double curTotal = current.stream().mapToInt(p -> p.articleCount).sum();
        double prevTotal = previous.stream().mapToInt(p -> p.articleCount).sum();

        if (prevTotal == 0 && curTotal == 0) return "近两周期均无发文，热度较稳定。";
        if (prevTotal == 0) return "上一周期无发文，本周期新增 " + (int) curTotal + " 篇文章。";

        double change = (curTotal - prevTotal) / prevTotal * 100;
        String dir = change > 0 ? "上升" : change < 0 ? "下降" : "持平";
        String peak = current.stream()
                .max(Comparator.comparingInt(p -> p.articleCount))
                .map(p -> p.label).orElse("");

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("该标签本周期发文量较上周期%s %.0f%%。", dir, Math.abs(change)));
        if (!peak.isEmpty()) sb.append(String.format(" 峰值出现在 %s。", peak));
        return sb.toString();
    }

    // ==================== helpers ====================

    private String resolveBucket(LocalDate date, String granularity) {
        return switch (granularity) {
            case "week" -> date.format(WEEK_FMT);
            case "day" -> date.format(DAY_FMT);
            default -> date.format(MONTH_FMT);
        };
    }

    // ==================== 数据类 ====================

    public record TagRawMetrics(Long tagId, String tagName, long views, long likes, long comments, int articles) {}

    public record TagHotResult(Long tagId, String tagName, long heatIndex,
                                long views, long likes, long comments, int articles) {}

    public static class TimeSeriesPoint {
        public String label;
        public int articleCount;
        public int viewCount;
        public int likeCount;
        public int commentCount;

        public TimeSeriesPoint(String label, int articleCount, int viewCount, int likeCount, int commentCount) {
            this.label = label;
            this.articleCount = articleCount;
            this.viewCount = viewCount;
            this.likeCount = likeCount;
            this.commentCount = commentCount;
        }
    }
}
