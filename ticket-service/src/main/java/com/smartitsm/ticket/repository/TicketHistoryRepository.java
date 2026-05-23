package com.smartitsm.ticket.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartitsm.ticket.entity.TicketHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * TicketHistory repository using MyBatis Plus IService.
 */
@Mapper
public interface TicketHistoryRepository extends IService<TicketHistory> {
}