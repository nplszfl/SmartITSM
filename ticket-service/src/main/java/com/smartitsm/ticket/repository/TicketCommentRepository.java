package com.smartitsm.ticket.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartitsm.ticket.entity.TicketComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * TicketComment repository using MyBatis Plus IService.
 */
@Mapper
public interface TicketCommentRepository extends IService<TicketComment> {
}