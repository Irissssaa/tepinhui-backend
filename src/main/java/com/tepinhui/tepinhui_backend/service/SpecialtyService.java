package com.tepinhui.tepinhui_backend.service;

import com.tepinhui.tepinhui_backend.vo.specialty.*;

import java.util.List;

public interface SpecialtyService {

    /**
     * 获取特产分布地图数据
     * 按省份分组，返回各省份的特产列表
     */
    List<RegionSpecialtyVO> getSpecialtyMap();

    /**
     * 获取特产详情
     */
    SpecialtyVO getSpecialtyDetail(Long id);

    /**
     * 节气推荐
     * 根据当前月份匹配季节标签，返回推荐特产
     */
    SeasonRecommendVO getSeasonRecommend();
}
