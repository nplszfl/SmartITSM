package com.smartitsm.sla.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartitsm.sla.entity.KnowledgeRecommendation;
import org.apache.ibatis.annotations.Mapper;

/**
 * Knowledge recommendation repository.
 */
@Mapper
public interface KnowledgeRecommendationRepository extends IService<KnowledgeRecommendation> {
}