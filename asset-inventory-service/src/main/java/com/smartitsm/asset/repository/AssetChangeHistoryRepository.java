package com.smartitsm.asset.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartitsm.asset.entity.AssetChangeHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * Asset change history repository.
 */
@Mapper
public interface AssetChangeHistoryRepository extends IService<AssetChangeHistory> {
}