package com.smartitsm.knowledge.controller;

import com.smartitsm.common.dto.ApiResponse;
import com.smartitsm.knowledge.entity.KnowledgeCategory;
import com.smartitsm.knowledge.service.KnowledgeCategoryService;
import com.smartitsm.knowledge.service.KnowledgeCategoryService.CategoryTreeNode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Knowledge Category REST API Controller.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class KnowledgeCategoryController {

    private final KnowledgeCategoryService categoryService;

    /**
     * Create a new knowledge category.
     */
    @PostMapping
    public ApiResponse<KnowledgeCategory> createCategory(@RequestBody KnowledgeCategory category) {
        KnowledgeCategory created = categoryService.createCategory(category);
        return ApiResponse.ok(created);
    }

    /**
     * Update an existing knowledge category.
     */
    @PutMapping("/{id}")
    public ApiResponse<KnowledgeCategory> updateCategory(@PathVariable Long id,
                                                         @RequestBody KnowledgeCategory category) {
        KnowledgeCategory updated = categoryService.updateCategory(id, category);
        return ApiResponse.ok(updated);
    }

    /**
     * Delete a knowledge category.
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ApiResponse.ok(null);
    }

    /**
     * Get category by ID.
     */
    @GetMapping("/{id}")
    public ApiResponse<KnowledgeCategory> getCategory(@PathVariable Long id) {
        KnowledgeCategory category = categoryService.getCategory(id);
        return ApiResponse.ok(category);
    }

    /**
     * List all categories (optionally filtered by parentId).
     */
    @GetMapping
    public ApiResponse<List<KnowledgeCategory>> listCategories(
            @RequestParam(required = false) Long parentId) {
        List<KnowledgeCategory> categories = categoryService.listCategories(parentId);
        return ApiResponse.ok(categories);
    }

    /**
     * Get category tree - hierarchical structure.
     */
    @GetMapping("/tree")
    public ApiResponse<List<CategoryTreeNode>> getCategoryTree() {
        List<CategoryTreeNode> tree = categoryService.getCategoryTree();
        return ApiResponse.ok(tree);
    }
}