package com.smartitsm.workflow.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartitsm.workflow.entity.WorkflowInstance;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowInstanceRepository extends BaseMapper<WorkflowInstance> {
}
