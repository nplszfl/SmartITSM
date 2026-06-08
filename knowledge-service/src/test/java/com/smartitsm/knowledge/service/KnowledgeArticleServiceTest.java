package com.smartitsm.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartitsm.common.exception.ResourceNotFoundException;
import com.smartitsm.knowledge.dto.KnowledgeArticleRequest;
import com.smartitsm.knowledge.entity.KnowledgeArticle;
import com.smartitsm.knowledge.repository.KnowledgeArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for KnowledgeArticleService business logic.
 * Covers article statistics, expiration, duplicate detection, and helpfulness metrics.
 */
@ExtendWith(MockitoExtension.class)
class KnowledgeArticleServiceTest {

    @Mock
    private KnowledgeArticleRepository articleRepository;

    @InjectMocks
    private KnowledgeArticleService articleService;

    private KnowledgeArticle sampleArticle;

    @BeforeEach
    void setUp() {
        sampleArticle = KnowledgeArticle.builder()
            .articleNumber("KBA-202605-0001")
            .title("How to reset Windows password")
            .content("Step 1: Click Start...")
            .summary("Reset Windows password guide")
            .category("IT_SUPPORT")
            .subCategory("WINDOWS")
            .tags("windows,password,reset")
            .status("PUBLISHED")
            .authorId("user-001")
            .authorName("Admin")
            .viewCount(100)
            .helpfulCount(80)
            .notHelpfulCount(20)
            .publishedAt(LocalDateTime.now().minusDays(7))
            .build();
        sampleArticle.setId(1L);
        sampleArticle.setCreatedAt(LocalDateTime.now().minusDays(10));
        sampleArticle.setUpdatedAt(LocalDateTime.now().minusDays(7));
    }

    // ==================== Article Statistics ====================

    @Test
    void testGetStatistics_AggregatesCountsByStatus() {
        // Arrange
        when(articleRepository.count(any(LambdaQueryWrapper.class)))
            .thenReturn(10L, 5L, 3L, 2L);
        when(articleRepository.list()).thenReturn(Arrays.asList(sampleArticle));

        // Act
        Map<String, Object> stats = articleService.getStatistics();

        // Assert
        assertNotNull(stats);
        // total = published + draft + archived (expired is a subset of published)
        assertEquals(18L, stats.get("totalArticles"));
        assertEquals(10L, stats.get("publishedCount"));
        assertEquals(5L, stats.get("draftCount"));
        assertEquals(3L, stats.get("archivedCount"));
        assertEquals(2L, stats.get("expiredCount"));
    }

    @Test
    void testGetStatistics_CalculatesOverallHelpfulnessRatio() {
        // Arrange
        KnowledgeArticle a1 = KnowledgeArticle.builder().viewCount(100).helpfulCount(80).notHelpfulCount(20).build();
        KnowledgeArticle a2 = KnowledgeArticle.builder().viewCount(50).helpfulCount(40).notHelpfulCount(10).build();
        when(articleRepository.count(any(LambdaQueryWrapper.class))).thenReturn(2L, 0L, 0L, 0L);
        when(articleRepository.list()).thenReturn(Arrays.asList(a1, a2));

        // Act
        Map<String, Object> stats = articleService.getStatistics();

        // Assert
        assertNotNull(stats);
        // Total helpful = 120, total not helpful = 30, ratio = 120/150 = 80%
        assertEquals(80.0, ((Number) stats.get("helpfulnessRatio")).doubleValue());
    }

    @Test
    void testGetStatistics_HandlesEmptyDatabase() {
        // Arrange
        when(articleRepository.count(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(articleRepository.list()).thenReturn(Collections.emptyList());

        // Act
        Map<String, Object> stats = articleService.getStatistics();

        // Assert
        assertNotNull(stats);
        assertEquals(0L, stats.get("totalArticles"));
        assertEquals(0.0, ((Number) stats.get("helpfulnessRatio")).doubleValue());
    }

    // ==================== Auto-Expire Articles ====================

    @Test
    void testAutoExpireArticles_ArchivesPublishedExpiredArticles() {
        // Arrange
        KnowledgeArticle expired1 = KnowledgeArticle.builder().status("PUBLISHED")
            .expiresAt(LocalDateTime.now().minusDays(1)).build();
        expired1.setId(10L);
        KnowledgeArticle expired2 = KnowledgeArticle.builder().status("PUBLISHED")
            .expiresAt(LocalDateTime.now().minusDays(30)).build();
        expired2.setId(11L);
        when(articleRepository.list(any(QueryWrapper.class)))
            .thenReturn(Arrays.asList(expired1, expired2));

        // Act
        int count = articleService.autoExpireArticles();

        // Assert
        assertEquals(2, count);
        assertEquals("ARCHIVED", expired1.getStatus());
        assertEquals("ARCHIVED", expired2.getStatus());
        verify(articleRepository, times(2)).updateById(any(KnowledgeArticle.class));
    }

    @Test
    void testAutoExpireArticles_NoExpiredArticlesReturnsZero() {
        // Arrange
        when(articleRepository.list(any(QueryWrapper.class))).thenReturn(Collections.emptyList());

        // Act
        int count = articleService.autoExpireArticles();

        // Assert
        assertEquals(0, count);
        verify(articleRepository, never()).updateById(any(KnowledgeArticle.class));
    }

    // ==================== Duplicate Detection ====================

    @Test
    void testFindSimilarArticles_MatchesByTitle() {
        // Arrange
        KnowledgeArticle similar = KnowledgeArticle.builder()
            .title("How to reset Windows password quickly")
            .content("different content").status("PUBLISHED").build();
        similar.setId(2L);
        when(articleRepository.list(any(QueryWrapper.class)))
            .thenReturn(Arrays.asList(similar));

        // Act
        List<KnowledgeArticle> results = articleService.findSimilarArticles("reset Windows password", 5);

        // Assert
        assertEquals(1, results.size());
        assertEquals(2L, results.get(0).getId());
    }

    @Test
    void testFindSimilarArticles_BlankKeywordReturnsEmpty() {
        // Act
        List<KnowledgeArticle> results = articleService.findSimilarArticles("  ", 5);

        // Assert
        assertTrue(results.isEmpty());
        verify(articleRepository, never()).list(any(QueryWrapper.class));
    }

    @Test
    void testFindSimilarArticles_RespectsLimit() {
        // Arrange
        when(articleRepository.list(any(QueryWrapper.class))).thenReturn(Collections.emptyList());

        // Act
        List<KnowledgeArticle> results = articleService.findSimilarArticles("test", 3);

        // Assert
        assertNotNull(results);
    }

    // ==================== Helpfulness Metrics ====================

    @Test
    void testCalculateHelpfulnessRatio_ForPositiveFeedback() {
        // Arrange
        sampleArticle.setHelpfulCount(80);
        sampleArticle.setNotHelpfulCount(20);

        // Act
        double ratio = articleService.calculateHelpfulnessRatio(sampleArticle);

        // Assert
        assertEquals(80.0, ratio);
    }

    @Test
    void testCalculateHelpfulnessRatio_ForZeroFeedback() {
        // Arrange
        sampleArticle.setHelpfulCount(0);
        sampleArticle.setNotHelpfulCount(0);

        // Act
        double ratio = articleService.calculateHelpfulnessRatio(sampleArticle);

        // Assert
        assertEquals(0.0, ratio);
    }

    @Test
    void testCalculateHelpfulnessRatio_ForAllNegativeFeedback() {
        // Arrange
        sampleArticle.setHelpfulCount(0);
        sampleArticle.setNotHelpfulCount(100);

        // Act
        double ratio = articleService.calculateHelpfulnessRatio(sampleArticle);

        // Assert
        assertEquals(0.0, ratio);
    }

    // ==================== Article Count By Category ====================

    @Test
    void testCountByCategory_ReturnsCount() {
        // Arrange
        when(articleRepository.count(any(LambdaQueryWrapper.class))).thenReturn(42L);

        // Act
        long count = articleService.countByCategory("IT_SUPPORT");

        // Assert
        assertEquals(42L, count);
    }

    // ==================== Author Articles ====================

    @Test
    void testGetArticlesByAuthor_ReturnsAuthorArticles() {
        // Arrange
        when(articleRepository.list(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(sampleArticle));

        // Act
        List<KnowledgeArticle> results = articleService.getArticlesByAuthor("user-001");

        // Assert
        assertEquals(1, results.size());
        assertEquals("user-001", results.get(0).getAuthorId());
    }

    @Test
    void testGetArticlesByAuthor_NullAuthorReturnsEmpty() {
        // Act
        List<KnowledgeArticle> results = articleService.getArticlesByAuthor(null);

        // Assert
        assertTrue(results.isEmpty());
        verify(articleRepository, never()).list(any(QueryWrapper.class));
    }

    // ==================== Bulk Operations ====================

    @Test
    void testBulkUpdateStatus_UpdatesAllSpecifiedArticles() {
        // Arrange
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(articleRepository.listByIds(ids))
            .thenReturn(Arrays.asList(
                KnowledgeArticle.builder().status("DRAFT").build(),
                KnowledgeArticle.builder().status("DRAFT").build(),
                KnowledgeArticle.builder().status("DRAFT").build()
            ));

        // Act
        int count = articleService.bulkUpdateStatus(ids, "PUBLISHED");

        // Assert
        assertEquals(3, count);
        verify(articleRepository, times(3)).updateById(any(KnowledgeArticle.class));
    }

    @Test
    void testBulkUpdateStatus_EmptyListReturnsZero() {
        // Act
        int count = articleService.bulkUpdateStatus(Collections.emptyList(), "PUBLISHED");

        // Assert
        assertEquals(0, count);
        verify(articleRepository, never()).listByIds(any());
    }

    // ==================== Top Contributors ====================

    @Test
    void testGetTopContributors_GroupsByAuthor() {
        // Arrange
        KnowledgeArticle a1 = KnowledgeArticle.builder().authorId("u1").authorName("Alice").build();
        KnowledgeArticle a2 = KnowledgeArticle.builder().authorId("u1").authorName("Alice").build();
        KnowledgeArticle a3 = KnowledgeArticle.builder().authorId("u2").authorName("Bob").build();
        when(articleRepository.list()).thenReturn(Arrays.asList(a1, a2, a3));

        // Act
        List<Map<String, Object>> top = articleService.getTopContributors(5);

        // Assert
        assertEquals(2, top.size());
        assertEquals("u1", top.get(0).get("authorId"));
        assertEquals(2L, top.get(0).get("articleCount"));
        assertEquals("u2", top.get(1).get("authorId"));
        assertEquals(1L, top.get(1).get("articleCount"));
    }

    @Test
    void testGetTopContributors_RespectsLimit() {
        // Arrange
        when(articleRepository.list()).thenReturn(Collections.emptyList());

        // Act
        List<Map<String, Object>> top = articleService.getTopContributors(3);

        // Assert
        assertTrue(top.isEmpty());
    }

    // ==================== Article Number Generation ====================

    @Test
    void testGenerateArticleNumber_FollowsKbaFormat() {
        // Act
        String number = articleService.generateArticleNumber();

        // Assert
        assertNotNull(number);
        assertTrue(number.startsWith("KBA-"));
        assertTrue(number.matches("KBA-\\d{6}-\\d{4}"));
    }

    @Test
    void testGenerateArticleNumber_ProducesUniqueValues() {
        // Act
        String n1 = articleService.generateArticleNumber();
        String n2 = articleService.generateArticleNumber();

        // Assert
        assertNotEquals(n1, n2);
    }

    // ==================== Update Validation ====================

    @Test
    void testUpdateArticle_ThrowsWhenArticleNotFound() {
        // Arrange
        when(articleRepository.getById(99L)).thenReturn(null);

        KnowledgeArticleRequest request = KnowledgeArticleRequest.builder().title("X").build();

        // Act + Assert
        assertThrows(ResourceNotFoundException.class,
            () -> articleService.updateArticle(99L, request));
    }

    @Test
    void testUpdateArticle_RejectsBlankTitle() {
        // Arrange
        when(articleRepository.getById(1L)).thenReturn(sampleArticle);

        KnowledgeArticleRequest request = KnowledgeArticleRequest.builder().title("  ").build();

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
            () -> articleService.updateArticle(1L, request));
    }

    // ==================== Article Creation Validation ====================

    @Test
    void testCreateArticle_RejectsBlankTitle() {
        // Arrange
        KnowledgeArticleRequest request = KnowledgeArticleRequest.builder()
            .title("")
            .content("content")
            .build();

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
            () -> articleService.createArticle(request));
    }

    @Test
    void testCreateArticle_RejectsBlankContent() {
        // Arrange
        KnowledgeArticleRequest request = KnowledgeArticleRequest.builder()
            .title("Valid Title")
            .content("")
            .build();

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
            () -> articleService.createArticle(request));
    }

    @Test
    void testCreateArticle_PersistsWithDefaults() {
        // Arrange
        KnowledgeArticleRequest request = KnowledgeArticleRequest.builder()
            .title("Valid Title")
            .content("Valid content here")
            .authorId("u-1")
            .authorName("Author")
            .build();

        when(articleRepository.save(any(KnowledgeArticle.class))).thenReturn(true);

        ArgumentCaptor<KnowledgeArticle> captor = ArgumentCaptor.forClass(KnowledgeArticle.class);

        // Act
        KnowledgeArticle result = articleService.createArticle(request);

        // Assert
        verify(articleRepository).save(captor.capture());
        KnowledgeArticle saved = captor.getValue();
        assertEquals("Valid Title", saved.getTitle());
        assertEquals("DRAFT", saved.getStatus());
        assertEquals(0, saved.getViewCount());
        assertEquals(0, saved.getHelpfulCount());
        assertEquals(0, saved.getResolutionRate());
        assertNotNull(saved.getArticleNumber());
        assertTrue(saved.getArticleNumber().startsWith("KBA-"));
    }
}
