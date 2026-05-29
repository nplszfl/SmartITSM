package com.smartitsm.sla.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartitsm.sla.entity.SlaCompliance;
import org.apache.ibatis.annotations.Mapper;

/**
 * SLA compliance repository.
 */
@Mapper
public interface SlaComplianceRepository extends IService<SlaCompliance> {
}