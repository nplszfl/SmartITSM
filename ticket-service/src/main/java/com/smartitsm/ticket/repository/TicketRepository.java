package com.smartitsm.ticket.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartitsm.ticket.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;

/**
 * Ticket repository using MyBatis Plus.
 */
@Mapper
public interface TicketRepository extends BaseMapper<Ticket> {
}