package com.smartitsm.sla.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartitsm.sla.entity.SlaBreachRisk;
import org.apache.ibatis.annotations.Mapper;

/**
 * SLA breach risk repository.
 */
@Mapper
public interface SlaBreachRiskRepository extends IService<SlaBreachRisk> {
}