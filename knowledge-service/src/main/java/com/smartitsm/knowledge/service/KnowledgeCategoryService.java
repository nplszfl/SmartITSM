package com.smartitsm.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartitsm.knowledge.entity.KnowledgeCategory;
import com.smartitsm.knowledge.repository.KnowledgeCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Knowledge Category Service - handles knowledge category business logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeCategoryService {

    private final KnowledgeCategoryRepository categoryRepository;

    /**
     * Create a new knowledge category.
     */
    @Transactional
    public KnowledgeCategory createCategory(KnowledgeCategory category) {
        log.info("Creating knowledge category: {}", category.getName());
        
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        if (category.getArticleCount() == null) {
            category.setArticleCount(0);
        }
        
        categoryRepository.save(category);
        log.info("Created knowledge category {} with id {}", category.getName(), category.getId());
        
        return category;
    }

    /**
     * Update an existing knowledge category.
     */
    @Transactional
    public KnowledgeCategory updateCategory(Long id, KnowledgeCategory category) {
        log.info("Updating knowledge category: {}", id);
        
        KnowledgeCategory existing = categoryRepository.getById(id);
        if (existing == null) {
            throw new RuntimeException("Category not found: " + id);
        }
        
        existing.setName(category.getName());
        existing.setDescription(category.getDescription());
        existing.setParentId(category.getParentId());
        existing.setSortOrder(category.getSortOrder());
        existing.setIcon(category.getIcon());
        existing.setColor(category.getColor());
        
        categoryRepository.updateById(existing);
        log.info("Updated knowledge category {}", id);
        
        return existing;
    }

    /**
     * Delete a knowledge category.
     */
    @Transactional
    public void deleteCategory(Long id) {
        log.info("Deleting knowledge category: {}", id);
        categoryRepository.removeById(id);
    }

    /**
     * Get category by ID.
     */
    public KnowledgeCategory getCategory(Long id) {
        return categoryRepository.getById(id);
    }

    /**
     * List all categories, optionally filtered by parentId.
     */
    public List<KnowledgeCategory> listCategories(Long parentId) {
        QueryWrapper<KnowledgeCategory> query = new QueryWrapper<>();
        
        if (parentId != null) {
            query.eq("parent_id", parentId);
        }
        
        query.orderByAsc("sort_order", "name");
        
        return categoryRepository.list(query);
    }

    /**
     * Get category tree - hierarchical structure.
     */
    public List<CategoryTreeNode> getCategoryTree() {
        List<KnowledgeCategory> allCategories = categoryRepository.list(
            new QueryWrapper<KnowledgeCategory>().orderByAsc("sort_order", "name")
        );
        
        Map<Long, List<KnowledgeCategory>> childrenMap = allCategories.stream()
            .filter(c -> c.getParentId() != null)
            .collect(Collectors.groupingBy(KnowledgeCategory::getParentId));
        
        List<CategoryTreeNode> roots = new ArrayList<>();
        
        for (KnowledgeCategory category : allCategories) {
            if (category.getParentId() == null) {
                roots.add(buildTreeNode(category, childrenMap));
            }
        }
        
        return roots;
    }

    /**
     * Update article count for a category.
     */
    @Transactional
    public void updateArticleCount(Long categoryId) {
        log.info("Updating article count for category: {}", categoryId);
        // This would typically be called when articles are created/updated/deleted
        // For now, we just log the action - actual count update would be handled by the article service
    }

    /**
     * Build tree node recursively.
     */
    private CategoryTreeNode buildTreeNode(KnowledgeCategory category, 
                                          Map<Long, List<KnowledgeCategory>> childrenMap) {
        CategoryTreeNode node = new CategoryTreeNode();
        node.setId(category.getId());
        node.setName(category.getName());
        node.setDescription(category.getDescription());
        node.setParentId(category.getParentId());
        node.setSortOrder(category.getSortOrder());
        node.setArticleCount(category.getArticleCount());
        node.setIcon(category.getIcon());
        node.setColor(category.getColor());
        
        List<KnowledgeCategory> children = childrenMap.get(category.getId());
        if (children != null && !children.isEmpty()) {
            List<CategoryTreeNode> childNodes = children.stream()
                .map(child -> buildTreeNode(child, childrenMap))
                .collect(Collectors.toList());
            node.setChildren(childNodes);
        }
        
        return node;
    }

    /**
     * Category tree node for hierarchical representation.
     */
    public static class CategoryTreeNode {
        private Long id;
        private String name;
        private String description;
        private Long parentId;
        private Integer sortOrder;
        private Integer articleCount;
        private String icon;
        private String color;
        private List<CategoryTreeNode> children;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Long getParentId() { return parentId; }
        public void setParentId(Long parentId) { this.parentId = parentId; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
        public Integer getArticleCount() { return articleCount; }
        public void setArticleCount(Integer articleCount) { this.articleCount = articleCount; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public List<CategoryTreeNode> getChildren() { return children; }
        public void setChildren(List<CategoryTreeNode> children) { this.children = children; }
    }
}