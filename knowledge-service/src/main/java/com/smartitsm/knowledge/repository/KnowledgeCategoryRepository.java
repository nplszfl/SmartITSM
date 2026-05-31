package com.smartitsm.knowledge.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartitsm.knowledge.entity.KnowledgeCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * Knowledge Category Repository - MyBatis Plus mapper for KnowledgeCategory.
 */
@Mapper
public interface KnowledgeCategoryRepository extends IService<KnowledgeCategory> {
}