package com.smartitsm.asset.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartitsm.asset.entity.InventoryTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * Inventory task repository.
 */
@Mapper
public interface InventoryTaskRepository extends IService<InventoryTask> {
}