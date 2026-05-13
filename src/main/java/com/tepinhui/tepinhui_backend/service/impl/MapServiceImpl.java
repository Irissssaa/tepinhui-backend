package com.tepinhui.tepinhui_backend.service.impl;

import com.tepinhui.tepinhui_backend.service.MapService;
import com.tepinhui.tepinhui_backend.vo.map.SeasonRecommendVO;
import com.tepinhui.tepinhui_backend.vo.map.SpecialtyMapVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MapServiceImpl implements MapService {

    // 预留缓存 key: stats:specialty:map
    private static final String SPECIALTY_MAP_CACHE_KEY = "stats:specialty:map";

    @Override
    public List<SpecialtyMapVO> listSpecialties() {
        return List.of();
    }

    @Override
    public List<SeasonRecommendVO> listSeasonRecommendations() {
        return List.of();
    }
}
