package com.smartitsm.knowledge.controller;

import com.smartitsm.common.dto.ApiResponse;
import com.smartitsm.knowledge.dto.KnowledgeArticleRequest;
import com.smartitsm.knowledge.entity.KnowledgeArticle;
import com.smartitsm.knowledge.service.KnowledgeArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Knowledge Article REST API Controller.
 */
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class KnowledgeArticleController {

    private final KnowledgeArticleService articleService;

    /**
     * Create a new knowledge article.
     */
    @PostMapping
    public ApiResponse<KnowledgeArticle> createArticle(@RequestBody KnowledgeArticleRequest request) {
        KnowledgeArticle article = articleService.createArticle(request);
        return ApiResponse.ok(article);
    }

    /**
     * Update an existing knowledge article.
     */
    @PutMapping("/{id}")
    public ApiResponse<KnowledgeArticle> updateArticle(@PathVariable Long id, 
                                                        @RequestBody KnowledgeArticleRequest request) {
        KnowledgeArticle article = articleService.updateArticle(id, request);
        return ApiResponse.ok(article);
    }

    /**
     * Publish a knowledge article.
     */
    @PatchMapping("/{id}/publish")
    public ApiResponse<KnowledgeArticle> publishArticle(@PathVariable Long id,
                                                         @RequestParam String approvedBy) {
        KnowledgeArticle article = articleService.publishArticle(id, approvedBy);
        return ApiResponse.ok(article);
    }

    /**
     * Archive a knowledge article.
     */
    @PatchMapping("/{id}/archive")
    public ApiResponse<KnowledgeArticle> archiveArticle(@PathVariable Long id) {
        KnowledgeArticle article = articleService.archiveArticle(id);
        return ApiResponse.ok(article);
    }

    /**
     * Delete a knowledge article.
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return ApiResponse.ok(null);
    }

    /**
     * Get article by ID.
     */
    @GetMapping("/{id}")
    public ApiResponse<KnowledgeArticle> getArticle(@PathVariable Long id) {
        KnowledgeArticle article = articleService.getArticle(id);
        return ApiResponse.ok(article);
    }

    /**
     * Get article by article number.
     */
    @GetMapping("/number/{articleNumber}")
    public ApiResponse<KnowledgeArticle> getArticleByNumber(@PathVariable String articleNumber) {
        KnowledgeArticle article = articleService.getArticleByNumber(articleNumber);
        return ApiResponse.ok(article);
    }

    /**
     * Search articles with keyword, category, status, page, size.
     */
    @GetMapping("/search")
    public ApiResponse<List<KnowledgeArticle>> searchArticles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<KnowledgeArticle> articles = articleService.searchArticles(keyword, category, status, page, size);
        return ApiResponse.ok(articles);
    }

    /**
     * Get popular articles.
     */
    @GetMapping("/popular")
    public ApiResponse<List<KnowledgeArticle>> getPopularArticles(@RequestParam(defaultValue = "10") int limit) {
        List<KnowledgeArticle> articles = articleService.getPopularArticles(limit);
        return ApiResponse.ok(articles);
    }

    /**
     * Get articles by category.
     */
    @GetMapping("/category/{categoryId}")
    public ApiResponse<List<KnowledgeArticle>> getArticlesByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<KnowledgeArticle> articles = articleService.getArticlesByCategory(categoryId, page, size);
        return ApiResponse.ok(articles);
    }

    /**
     * Get articles by tag.
     */
    @GetMapping("/tag/{tag}")
    public ApiResponse<List<KnowledgeArticle>> getArticlesByTag(
            @PathVariable String tag,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<KnowledgeArticle> articles = articleService.getArticlesByTag(tag, page, size);
        return ApiResponse.ok(articles);
    }

    /**
     * Get FAQ articles.
     */
    @GetMapping("/faq")
    public ApiResponse<List<KnowledgeArticle>> getFaqArticles() {
        List<KnowledgeArticle> articles = articleService.getFaqArticles();
        return ApiResponse.ok(articles);
    }

    /**
     * Increment view count.
     */
    @PostMapping("/{id}/view")
    public ApiResponse<Void> incrementViewCount(@PathVariable Long id) {
        articleService.incrementViewCount(id);
        return ApiResponse.ok(null);
    }

    /**
     * Mark article as helpful.
     */
    @PostMapping("/{id}/helpful")
    public ApiResponse<Void> markHelpful(@PathVariable Long id) {
        articleService.markHelpful(id);
        return ApiResponse.ok(null);
    }

    /**
     * Mark article as not helpful.
     */
    @PostMapping("/{id}/not-helpful")
    public ApiResponse<Void> markNotHelpful(@PathVariable Long id) {
        articleService.markNotHelpful(id);
        return ApiResponse.ok(null);
    }

    /**
     * Get related articles for a ticket.
     */
    @GetMapping("/related/ticket/{ticketId}")
    public ApiResponse<List<KnowledgeArticle>> getRelatedArticles(
            @PathVariable String ticketId,
            @RequestParam(defaultValue = "5") int limit) {
        List<KnowledgeArticle> articles = articleService.getRelatedArticles(ticketId, limit);
        return ApiResponse.ok(articles);
    }
}