package com.lanou.springaidemo;

import com.lanou.springaidemo.service.impl.TagAnalyticsService;
import com.lanou.springaidemo.service.impl.TagAnalyticsService.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TagAnalyticsServiceTest {

    private final TagAnalyticsService service = new TagAnalyticsService(null, null, null, null);

    @Test
    void heatIndex_shouldUseActualWeightedMetrics() {
        List<TagRawMetrics> raw = List.of(
                new TagRawMetrics(1L, "Java", 100, 10, 5, 3),
                new TagRawMetrics(2L, "Python", 50, 5, 2, 1)
        );
        List<TagHotResult> results = service.computeHeatIndexes(raw);
        assertEquals(2, results.size());
        assertEquals(185, results.get(0).heatIndex());
        assertEquals(85, results.get(1).heatIndex());
    }

    @Test
    void heatIndex_singleTag_shouldNotBecomeArtificialFullScore() {
        List<TagRawMetrics> raw = List.of(
                new TagRawMetrics(1L, "Java", 10, 1, 0, 1)
        );
        List<TagHotResult> results = service.computeHeatIndexes(raw);
        assertEquals(1, results.size());
        assertEquals(23, results.get(0).heatIndex());
    }

    @Test
    void heatIndex_emptyInput_returnsEmpty() {
        List<TagHotResult> results = service.computeHeatIndexes(List.of());
        assertTrue(results.isEmpty());
    }

    @Test
    void heatIndex_zeroData_shouldNotCrash() {
        List<TagRawMetrics> raw = List.of(
                new TagRawMetrics(1L, "Empty", 0, 0, 0, 0)
        );
        List<TagHotResult> results = service.computeHeatIndexes(raw);
        assertEquals(1, results.size());
        assertEquals(0, results.get(0).heatIndex());
    }

    @Test
    void trendAnalysis_emptyData_returnsInsufficientMessage() {
        String result = service.computeTrendAnalysis(List.of(), List.of());
        assertTrue(result.contains("不足"));
    }

    @Test
    void trendAnalysis_curOnly_withoutPrevious() {
        List<TimeSeriesPoint> current = List.of(new TimeSeriesPoint("2024-07", 3, 0, 0, 0));
        String result = service.computeTrendAnalysis(current, List.of());
        assertTrue(result.contains("上一周期"));
    }

    @Test
    void trendAnalysis_momIncrease() {
        List<TimeSeriesPoint> prev = List.of(new TimeSeriesPoint("2024-06", 2, 0, 0, 0));
        List<TimeSeriesPoint> cur = List.of(new TimeSeriesPoint("2024-07", 4, 0, 0, 0));
        String result = service.computeTrendAnalysis(cur, prev);
        assertTrue(result.contains("发文量"));
        assertTrue(result.contains("上升"));
        assertTrue(result.contains("100"));
    }
}
