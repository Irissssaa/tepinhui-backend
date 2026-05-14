package com.tepinhui.tepinhui_backend.vo.specialty;

import lombok.Data;
import java.util.List;

@Data
public class RegionSpecialtyVO {
    private String name;
    private Integer count;
    private List<SpecialtySimpleVO> specialties;
}
