package com.smartitsm.asset.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartitsm.asset.entity.Asset;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Asset repository - data access layer for IT assets.
 */
@Mapper
public interface AssetRepository extends BaseMapper<Asset> {

    @Select("SELECT * FROM t_asset WHERE asset_number = #{assetNumber} AND deleted = 0 LIMIT 1")
    Asset findByAssetNumber(@Param("assetNumber") String assetNumber);

    @Select("SELECT * FROM t_asset WHERE status = #{status} AND deleted = 0 ORDER BY updated_at DESC")
    List<Asset> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM t_asset WHERE asset_type = #{assetType} AND deleted = 0 ORDER BY updated_at DESC")
    List<Asset> findByAssetType(@Param("assetType") String assetType);

    @Select("SELECT * FROM t_asset WHERE health_status = #{healthStatus} AND deleted = 0")
    List<Asset> findByHealthStatus(@Param("healthStatus") String healthStatus);

    @Select("SELECT * FROM t_asset WHERE assigned_group = #{assignedGroup} AND deleted = 0 ORDER BY updated_at DESC")
    List<Asset> findByAssignedGroup(@Param("assignedGroup") String assignedGroup);

    @Select("SELECT * FROM t_asset WHERE risk_level = #{riskLevel} AND deleted = 0")
    List<Asset> findByRiskLevel(@Param("riskLevel") String riskLevel);

    @Select("SELECT * FROM t_asset WHERE parent_asset_id = #{parentAssetId} AND deleted = 0")
    List<Asset> findByParentAssetId(@Param("parentAssetId") Long parentAssetId);

    @Select("SELECT * FROM t_asset WHERE deleted = 0 ORDER BY updated_at DESC LIMIT #{limit}")
    List<Asset> findRecentAssets(@Param("limit") int limit);

    @Select("SELECT * FROM t_asset WHERE health_score < #{threshold} AND deleted = 0 ORDER BY health_score ASC")
    List<Asset> findAssetsBelowHealthScore(@Param("threshold") Double threshold);

    @Select("SELECT COUNT(*) FROM t_asset WHERE status = #{status} AND deleted = 0")
    long countByStatus(@Param("status") String status);

    @Select("SELECT COUNT(*) FROM t_asset WHERE asset_type = #{assetType} AND deleted = 0")
    long countByAssetType(@Param("assetType") String assetType);

    @Select("<script>" +
            "SELECT * FROM t_asset WHERE deleted = 0 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (name LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%') " +
            "OR asset_number LIKE CONCAT('%', #{keyword}, '%') OR serial_number LIKE CONCAT('%', #{keyword}, '%'))" +
            "</if>" +
            " ORDER BY updated_at DESC" +
            "</script>")
    IPage<Asset> searchAssets(Page<Asset> page, @Param("keyword") String keyword);
}
