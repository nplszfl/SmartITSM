package com.smartitsm.workflow.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartitsm.workflow.entity.WorkflowStep;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowStepRepository extends BaseMapper<WorkflowStep> {
}
