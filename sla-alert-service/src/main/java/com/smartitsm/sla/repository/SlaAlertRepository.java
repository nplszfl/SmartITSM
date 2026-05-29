package com.smartitsm.sla.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartitsm.sla.entity.SlaAlert;
import org.apache.ibatis.annotations.Mapper;

/**
 * SLA alert repository.
 */
@Mapper
public interface SlaAlertRepository extends IService<SlaAlert> {
}