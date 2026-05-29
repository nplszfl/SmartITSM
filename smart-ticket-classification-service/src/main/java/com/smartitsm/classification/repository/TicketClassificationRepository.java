package com.smartitsm.classification.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartitsm.classification.entity.TicketClassification;
import org.apache.ibatis.annotations.Mapper;

/**
 * Ticket classification repository.
 */
@Mapper
public interface TicketClassificationRepository extends IService<TicketClassification> {
}