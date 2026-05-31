package com.smartitsm.knowledge.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartitsm.knowledge.entity.KnowledgeArticle;
import org.apache.ibatis.annotations.Mapper;

/**
 * Knowledge Article Repository - MyBatis Plus mapper for KnowledgeArticle.
 */
@Mapper
public interface KnowledgeArticleRepository extends IService<KnowledgeArticle> {
}