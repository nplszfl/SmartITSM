package com.smartitsm.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartitsm.common.exception.ResourceNotFoundException;
import com.smartitsm.knowledge.dto.KnowledgeArticleRequest;
import com.smartitsm.knowledge.entity.KnowledgeArticle;
import com.smartitsm.knowledge.repository.KnowledgeArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Knowledge Article Service - handles knowledge article business logic.
 * Provides CRUD, search, statistics, helpfulness metrics, expiration, and
 * duplicate-detection operations for the ITSM knowledge base.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeArticleService {

    private final KnowledgeArticleRepository articleRepository;

    private final AtomicLong articleSequence = new AtomicLong(System.currentTimeMillis() % 10000);

    // ==================== CRUD ====================

    /**
     * Create a new knowledge article with validation.
     */
    @Transactional
    public KnowledgeArticle createArticle(KnowledgeArticleRequest request) {
        log.info("Creating knowledge article: {}", request.getTitle());

        validateArticleRequest(request);

        String articleNumber = generateArticleNumber();

        KnowledgeArticle article = KnowledgeArticle.builder()
            .articleNumber(articleNumber)
            .title(request.getTitle().trim())
            .content(request.getContent())
            .summary(request.getSummary())
            .category(request.getCategory())
            .subCategory(request.getSubCategory())
            .tags(request.getTags())
            .status(request.getStatus() != null ? request.getStatus() : "DRAFT")
            .authorId(request.getAuthorId())
            .authorName(request.getAuthorName())
            .relatedTicketIds(request.getRelatedTicketIds())
            .relatedAssetTypes(request.getRelatedAssetTypes())
            .expiresAt(request.getExpiresAt())
            .viewCount(0)
            .helpfulCount(0)
            .notHelpfulCount(0)
            .source("INTERNAL")
            .resolutionRate(0)
            .build();

        articleRepository.save(article);
        log.info("Created knowledge article {} with number {}", article.getId(), articleNumber);

        return article;
    }

    /**
     * Update an existing knowledge article.
     */
    @Transactional
    public KnowledgeArticle updateArticle(Long id, KnowledgeArticleRequest request) {
        log.info("Updating knowledge article: {}", id);

        KnowledgeArticle article = articleRepository.getById(id);
        if (article == null) {
            throw new ResourceNotFoundException("Article not found: " + id);
        }

        if (request.getTitle() != null && request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title must not be blank");
        }
        if (request.getContent() != null && request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Content must not be blank");
        }

        if (request.getTitle() != null) article.setTitle(request.getTitle().trim());
        if (request.getContent() != null) article.setContent(request.getContent());
        if (request.getSummary() != null) article.setSummary(request.getSummary());
        if (request.getCategory() != null) article.setCategory(request.getCategory());
        if (request.getSubCategory() != null) article.setSubCategory(request.getSubCategory());
        if (request.getTags() != null) article.setTags(request.getTags());
        if (request.getStatus() != null) article.setStatus(request.getStatus());
        if (request.getAuthorId() != null) article.setAuthorId(request.getAuthorId());
        if (request.getAuthorName() != null) article.setAuthorName(request.getAuthorName());
        if (request.getRelatedTicketIds() != null) article.setRelatedTicketIds(request.getRelatedTicketIds());
        if (request.getRelatedAssetTypes() != null) article.setRelatedAssetTypes(request.getRelatedAssetTypes());
        if (request.getExpiresAt() != null) article.setExpiresAt(request.getExpiresAt());

        articleRepository.updateById(article);
        log.info("Updated knowledge article {}", id);

        return article;
    }

    /**
     * Publish a knowledge article (DRAFT -> PUBLISHED).
     */
    @Transactional
    public KnowledgeArticle publishArticle(Long id, String approvedBy) {
        log.info("Publishing knowledge article: {} by {}", id, approvedBy);

        KnowledgeArticle article = articleRepository.getById(id);
        if (article == null) {
            throw new ResourceNotFoundException("Article not found: " + id);
        }

        article.setStatus("PUBLISHED");
        article.setApprovedBy(approvedBy);
        article.setApprovedAt(LocalDateTime.now());
        article.setPublishedAt(LocalDateTime.now());

        articleRepository.updateById(article);
        log.info("Published knowledge article {}", id);

        return article;
    }

    /**
     * Archive a knowledge article.
     */
    @Transactional
    public KnowledgeArticle archiveArticle(Long id) {
        log.info("Archiving knowledge article: {}", id);

        KnowledgeArticle article = articleRepository.getById(id);
        if (article == null) {
            throw new ResourceNotFoundException("Article not found: " + id);
        }

        article.setStatus("ARCHIVED");
        articleRepository.updateById(article);
        log.info("Archived knowledge article {}", id);

        return article;
    }

    /**
     * Delete a knowledge article (soft delete via MyBatis Plus).
     */
    @Transactional
    public void deleteArticle(Long id) {
        log.info("Deleting knowledge article: {}", id);
        articleRepository.removeById(id);
    }

    /**
     * Get article by ID.
     */
    public KnowledgeArticle getArticle(Long id) {
        return articleRepository.getById(id);
    }

    /**
     * Get article by article number.
     */
    public KnowledgeArticle getArticleByNumber(String articleNumber) {
        return articleRepository.getOne(
            new QueryWrapper<KnowledgeArticle>().eq("article_number", articleNumber)
        );
    }

    // ==================== Search & Listing ====================

    /**
     * Search articles by keyword in title and content.
     */
    public List<KnowledgeArticle> searchArticles(String keyword, String category,
                                                 String status, int page, int size) {
        QueryWrapper<KnowledgeArticle> query = new QueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            query.and(w -> w.like("title", keyword).or().like("content", keyword));
        }
        if (StringUtils.hasText(category)) {
            query.eq("category", category);
        }
        if (StringUtils.hasText(status)) {
            query.eq("status", status);
        } else {
            query.eq("status", "PUBLISHED");
        }

        query.orderByDesc("view_count", "created_at");

        Page<KnowledgeArticle> pageResult = articleRepository.page(new Page<>(page, size), query);
        return pageResult.getRecords();
    }

    /**
     * Get articles by category.
     */
    public List<KnowledgeArticle> getArticlesByCategory(String categoryId, int page, int size) {
        QueryWrapper<KnowledgeArticle> query = new QueryWrapper<>();
        query.eq("category", categoryId);
        query.eq("status", "PUBLISHED");
        query.orderByDesc("view_count", "created_at");

        Page<KnowledgeArticle> pageResult = articleRepository.page(new Page<>(page, size), query);
        return pageResult.getRecords();
    }

    /**
     * Get popular articles by view count.
     */
    public List<KnowledgeArticle> getPopularArticles(int limit) {
        QueryWrapper<KnowledgeArticle> query = new QueryWrapper<>();
        query.eq("status", "PUBLISHED");
        query.orderByDesc("view_count");
        query.last("LIMIT " + limit);

        return articleRepository.list(query);
    }

    /**
     * Get articles related to a ticket.
     */
    public List<KnowledgeArticle> getRelatedArticles(String ticketId, int limit) {
        QueryWrapper<KnowledgeArticle> query = new QueryWrapper<>();
        query.like("related_ticket_ids", ticketId);
        query.eq("status", "PUBLISHED");
        query.orderByDesc("view_count", "helpful_count");
        query.last("LIMIT " + limit);

        return articleRepository.list(query);
    }

    /**
     * Get articles by author (contributor).
     */
    public List<KnowledgeArticle> getArticlesByAuthor(String authorId) {
        if (!StringUtils.hasText(authorId)) {
            return Collections.emptyList();
        }
        return articleRepository.list(
            new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getAuthorId, authorId)
                .orderByDesc(KnowledgeArticle::getPublishedAt)
        );
    }

    /**
     * Get articles by tag.
     */
    public List<KnowledgeArticle> getArticlesByTag(String tag, int page, int size) {
        QueryWrapper<KnowledgeArticle> query = new QueryWrapper<>();
        query.like("tags", tag);
        query.eq("status", "PUBLISHED");
        query.orderByDesc("view_count", "created_at");

        Page<KnowledgeArticle> pageResult = articleRepository.page(new Page<>(page, size), query);
        return pageResult.getRecords();
    }

    /**
     * Get FAQ articles.
     */
    public List<KnowledgeArticle> getFaqArticles() {
        QueryWrapper<KnowledgeArticle> query = new QueryWrapper<>();
        query.eq("category", "FAQ");
        query.eq("status", "PUBLISHED");
        query.orderByDesc("helpful_count", "view_count");

        return articleRepository.list(query);
    }

    // ==================== Engagement Metrics ====================

    /**
     * Increment view count.
     */
    @Transactional
    public void incrementViewCount(Long id) {
        KnowledgeArticle article = articleRepository.getById(id);
        if (article != null) {
            article.setViewCount(article.getViewCount() == null ? 1 : article.getViewCount() + 1);
            articleRepository.updateById(article);
        }
    }

    /**
     * Mark article as helpful.
     */
    @Transactional
    public void markHelpful(Long id) {
        KnowledgeArticle article = articleRepository.getById(id);
        if (article != null) {
            article.setHelpfulCount(article.getHelpfulCount() == null ? 1 : article.getHelpfulCount() + 1);
            updateResolutionRate(article);
            articleRepository.updateById(article);
        }
    }

    /**
     * Mark article as not helpful.
     */
    @Transactional
    public void markNotHelpful(Long id) {
        KnowledgeArticle article = articleRepository.getById(id);
        if (article != null) {
            article.setNotHelpfulCount(article.getNotHelpfulCount() == null ? 1 : article.getNotHelpfulCount() + 1);
            updateResolutionRate(article);
            articleRepository.updateById(article);
        }
    }

    /**
     * Calculate the helpfulness ratio for an article (0-100).
     * Returns 0 when no feedback has been recorded.
     */
    public double calculateHelpfulnessRatio(KnowledgeArticle article) {
        if (article == null) {
            return 0.0;
        }
        int helpful = article.getHelpfulCount() == null ? 0 : article.getHelpfulCount();
        int notHelpful = article.getNotHelpfulCount() == null ? 0 : article.getNotHelpfulCount();
        int total = helpful + notHelpful;
        if (total == 0) {
            return 0.0;
        }
        return Math.round(((double) helpful / total) * 100.0 * 100.0) / 100.0;
    }

    // ==================== Statistics Dashboard ====================

    /**
     * Aggregate statistics for the knowledge base dashboard.
     * Includes counts by status, helpfulness ratio, and per-category breakdown.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long published = articleRepository.count(
            new LambdaQueryWrapper<KnowledgeArticle>().eq(KnowledgeArticle::getStatus, "PUBLISHED"));
        long draft = articleRepository.count(
            new LambdaQueryWrapper<KnowledgeArticle>().eq(KnowledgeArticle::getStatus, "DRAFT"));
        long archived = articleRepository.count(
            new LambdaQueryWrapper<KnowledgeArticle>().eq(KnowledgeArticle::getStatus, "ARCHIVED"));
        long expired = articleRepository.count(
            new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, "PUBLISHED")
                .lt(KnowledgeArticle::getExpiresAt, LocalDateTime.now()));

        long total = published + draft + archived;
        stats.put("totalArticles", total);
        stats.put("publishedCount", published);
        stats.put("draftCount", draft);
        stats.put("archivedCount", archived);
        stats.put("expiredCount", expired);
        stats.put("generatedAt", LocalDateTime.now());

        // Helpfulness ratio over all articles
        List<KnowledgeArticle> all = articleRepository.list();
        long totalHelpful = all.stream()
            .mapToLong(a -> a.getHelpfulCount() == null ? 0L : a.getHelpfulCount())
            .sum();
        long totalNotHelpful = all.stream()
            .mapToLong(a -> a.getNotHelpfulCount() == null ? 0L : a.getNotHelpfulCount())
            .sum();
        long totalFeedback = totalHelpful + totalNotHelpful;
        double ratio = totalFeedback == 0 ? 0.0 :
            Math.round(((double) totalHelpful / totalFeedback) * 100.0 * 100.0) / 100.0;
        stats.put("helpfulnessRatio", ratio);
        stats.put("totalHelpful", totalHelpful);
        stats.put("totalNotHelpful", totalNotHelpful);

        // Top categories by count
        Map<String, Long> byCategory = all.stream()
            .filter(a -> StringUtils.hasText(a.getCategory()))
            .collect(Collectors.groupingBy(
                KnowledgeArticle::getCategory, Collectors.counting()));
        stats.put("topCategories", byCategory);

        // Total views
        long totalViews = all.stream()
            .mapToLong(a -> a.getViewCount() == null ? 0L : a.getViewCount())
            .sum();
        stats.put("totalViews", totalViews);

        return stats;
    }

    /**
     * Get top contributors (authors) by article count.
     */
    public List<Map<String, Object>> getTopContributors(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        List<KnowledgeArticle> all = articleRepository.list();

        Map<String, Long> byAuthor = all.stream()
            .filter(a -> StringUtils.hasText(a.getAuthorId()))
            .collect(Collectors.groupingBy(
                KnowledgeArticle::getAuthorId, Collectors.counting()));

        return byAuthor.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(limit)
            .map(e -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("authorId", e.getKey());
                String authorName = all.stream()
                    .filter(a -> e.getKey().equals(a.getAuthorId()))
                    .map(KnowledgeArticle::getAuthorName)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse("");
                row.put("authorName", authorName);
                row.put("articleCount", e.getValue());
                return row;
            })
            .collect(Collectors.toList());
    }

    /**
     * Count published articles in a category.
     */
    public long countByCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return 0L;
        }
        return articleRepository.count(
            new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getCategory, category)
                .eq(KnowledgeArticle::getStatus, "PUBLISHED"));
    }

    // ==================== Auto-Expire ====================

    /**
     * Find articles that should be auto-archived (past expiration date and still published).
     */
    public List<KnowledgeArticle> getExpiredArticles() {
        QueryWrapper<KnowledgeArticle> query = new QueryWrapper<>();
        query.lt("expires_at", LocalDateTime.now());
        query.eq("status", "PUBLISHED");
        return articleRepository.list(query);
    }

    /**
     * Auto-archive all expired, currently published articles.
     * @return the number of articles that were archived
     */
    @Transactional
    public int autoExpireArticles() {
        List<KnowledgeArticle> expired = getExpiredArticles();
        if (expired.isEmpty()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        for (KnowledgeArticle article : expired) {
            article.setStatus("ARCHIVED");
            article.setUpdatedAt(now);
            articleRepository.updateById(article);
        }
        log.info("Auto-archived {} expired knowledge articles", expired.size());
        return expired.size();
    }

    // ==================== Duplicate Detection ====================

    /**
     * Find published articles whose title contains the given keyword.
     * Used as a basic duplicate-detection check before creating new articles.
     */
    public List<KnowledgeArticle> findSimilarArticles(String keyword, int limit) {
        if (!StringUtils.hasText(keyword) || limit <= 0) {
            return Collections.emptyList();
        }
        QueryWrapper<KnowledgeArticle> query = new QueryWrapper<>();
        query.like("title", keyword.trim());
        query.eq("status", "PUBLISHED");
        query.orderByDesc("view_count", "helpful_count");
        query.last("LIMIT " + limit);
        return articleRepository.list(query);
    }

    // ==================== Bulk Operations ====================

    /**
     * Bulk update status for a list of article IDs.
     * @return number of articles actually updated
     */
    @Transactional
    public int bulkUpdateStatus(List<Long> ids, String newStatus) {
        if (ids == null || ids.isEmpty() || !StringUtils.hasText(newStatus)) {
            return 0;
        }
        List<KnowledgeArticle> articles = articleRepository.listByIds(ids);
        if (articles.isEmpty()) {
            return 0;
        }
        LocalDateTime now = LocalDateTime.now();
        for (KnowledgeArticle article : articles) {
            article.setStatus(newStatus);
            article.setUpdatedAt(now);
            if ("PUBLISHED".equals(newStatus) && article.getPublishedAt() == null) {
                article.setPublishedAt(now);
            }
            articleRepository.updateById(article);
        }
        log.info("Bulk updated {} articles to status {}", articles.size(), newStatus);
        return articles.size();
    }

    // ==================== Article Number ====================

    /**
     * Generate a unique article number in format KBA-YYYYMM-XXXX.
     */
    public String generateArticleNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        long seq = articleSequence.incrementAndGet();
        return String.format("KBA-%s-%04d", date, seq % 10000);
    }

    // ==================== Private Helpers ====================

    private void validateArticleRequest(KnowledgeArticleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Content is required");
        }
    }

    private void updateResolutionRate(KnowledgeArticle article) {
        int helpful = article.getHelpfulCount() == null ? 0 : article.getHelpfulCount();
        int notHelpful = article.getNotHelpfulCount() == null ? 0 : article.getNotHelpfulCount();
        int total = helpful + notHelpful;
        if (total > 0) {
            article.setResolutionRate((int) Math.round(((double) helpful / total) * 100));
        }
    }
}
