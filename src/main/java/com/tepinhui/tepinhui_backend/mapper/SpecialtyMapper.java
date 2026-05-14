package com.tepinhui.tepinhui_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tepinhui.tepinhui_backend.entity.Specialty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SpecialtyMapper extends BaseMapper<Specialty> {

    /**
     * 查询所有上架特产（用于地图分布）
     */
    @Select("SELECT s.*, o.province_name, o.city_name, o.county_name, o.longitude, o.latitude " +
            "FROM specialty s " +
            "LEFT JOIN origin o ON s.origin_id = o.id " +
            "WHERE s.is_landing = 1 AND s.deleted = 0")
    List<Specialty> selectAllLandingSpecialties();

    /**
     * 根据省份查询特产
     */
    @Select("SELECT s.*, o.province_name, o.city_name, o.county_name, o.longitude, o.latitude " +
            "FROM specialty s " +
            "LEFT JOIN origin o ON s.origin_id = o.id " +
            "WHERE s.is_landing = 1 AND s.deleted = 0 " +
            "AND o.province_name = #{provinceName}")
    List<Specialty> selectByProvince(@Param("provinceName") String provinceName);

    /**
     * 根据季节标签查询特产
     */
    @Select("SELECT * FROM specialty WHERE is_landing = 1 AND deleted = 0 AND season_tag = #{seasonTag}")
    List<Specialty> selectBySeasonTag(@Param("seasonTag") String seasonTag);

    /**
     * 查询所有上架特产（分页用）
     */
    @Select("SELECT s.*, o.province_name, o.city_name, o.county_name, o.longitude, o.latitude " +
            "FROM specialty s " +
            "LEFT JOIN origin o ON s.origin_id = o.id " +
            "WHERE s.is_landing = 1 AND s.deleted = 0")
    List<Specialty> selectAllLanding();
}
