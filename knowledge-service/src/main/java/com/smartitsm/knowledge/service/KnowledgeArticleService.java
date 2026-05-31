package com.smartitsm.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartitsm.knowledge.dto.KnowledgeArticleRequest;
import com.smartitsm.knowledge.dto.KnowledgeArticleResponse;
import com.smartitsm.knowledge.dto.KnowledgeSearchResult;
import com.smartitsm.knowledge.entity.KnowledgeArticle;
import com.smartitsm.knowledge.repository.KnowledgeArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Knowledge Article Service - handles knowledge article business logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeArticleService {

    private final KnowledgeArticleRepository articleRepository;
    
    private final AtomicLong articleSequence = new AtomicLong(System.currentTimeMillis() % 10000);

    /**
     * Create a new knowledge article.
     */
    @Transactional
    public KnowledgeArticle createArticle(KnowledgeArticleRequest request) {
        log.info("Creating knowledge article: {}", request.getTitle());
        
        String articleNumber = generateArticleNumber();
        
        KnowledgeArticle article = KnowledgeArticle.builder()
            .articleNumber(articleNumber)
            .title(request.getTitle())
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
            throw new RuntimeException("Article not found: " + id);
        }
        
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setSummary(request.getSummary());
        article.setCategory(request.getCategory());
        article.setSubCategory(request.getSubCategory());
        article.setTags(request.getTags());
        if (request.getStatus() != null) {
            article.setStatus(request.getStatus());
        }
        article.setAuthorId(request.getAuthorId());
        article.setAuthorName(request.getAuthorName());
        article.setRelatedTicketIds(request.getRelatedTicketIds());
        article.setRelatedAssetTypes(request.getRelatedAssetTypes());
        article.setExpiresAt(request.getExpiresAt());
        
        articleRepository.updateById(article);
        log.info("Updated knowledge article {}", id);
        
        return article;
    }

    /**
     * Publish a knowledge article.
     */
    @Transactional
    public KnowledgeArticle publishArticle(Long id, String approvedBy) {
        log.info("Publishing knowledge article: {} by {}", id, approvedBy);
        
        KnowledgeArticle article = articleRepository.getById(id);
        if (article == null) {
            throw new RuntimeException("Article not found: " + id);
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
            throw new RuntimeException("Article not found: " + id);
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
            articleRepository.updateById(article);
        }
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

    /**
     * Get expired articles.
     */
    public List<KnowledgeArticle> getExpiredArticles() {
        QueryWrapper<KnowledgeArticle> query = new QueryWrapper<>();
        query.lt("expires_at", LocalDateTime.now());
        query.eq("status", "PUBLISHED");
        
        return articleRepository.list(query);
    }

    /**
     * Generate a unique article number in format KBA-YYYYMM-XXXX.
     */
    public String generateArticleNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        long seq = articleSequence.incrementAndGet();
        return String.format("KBA-%s-%04d", date, seq % 10000);
    }
}