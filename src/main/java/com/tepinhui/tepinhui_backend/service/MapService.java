package com.tepinhui.tepinhui_backend.service;

import com.tepinhui.tepinhui_backend.vo.map.SeasonRecommendVO;
import com.tepinhui.tepinhui_backend.vo.map.SpecialtyMapVO;

import java.util.List;

public interface MapService {

    List<SpecialtyMapVO> listSpecialties();

    List<SeasonRecommendVO> listSeasonRecommendations();
}
