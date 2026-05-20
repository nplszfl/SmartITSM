package com.smartitsm.workflow.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartitsm.workflow.entity.WorkflowDefinition;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowDefinitionRepository extends BaseMapper<WorkflowDefinition> {
}
