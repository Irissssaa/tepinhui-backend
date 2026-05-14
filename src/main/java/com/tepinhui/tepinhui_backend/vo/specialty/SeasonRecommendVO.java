package com.tepinhui.tepinhui_backend.vo.specialty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeasonRecommendVO {
    private String currentSeason;
    private Integer currentMonth;
    private List<SpecialtySimpleVO> recommendations;
}
