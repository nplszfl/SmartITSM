package com.smartitsm.ticket.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartitsm.ticket.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;

/**
 * Ticket repository using MyBatis Plus IService.
 */
@Mapper
public interface TicketRepository extends IService<Ticket> {
}
