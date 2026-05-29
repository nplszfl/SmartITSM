package com.smartitsm.asset.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartitsm.asset.entity.AssetInventory;
import org.apache.ibatis.annotations.Mapper;

/**
 * Asset inventory repository.
 */
@Mapper
public interface AssetInventoryRepository extends IService<AssetInventory> {
}