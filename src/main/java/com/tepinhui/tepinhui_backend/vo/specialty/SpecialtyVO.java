package com.tepinhui.tepinhui_backend.vo.specialty;

import lombok.Data;

@Data
public class SpecialtyVO {
    private Long id;
    private String name;
    private String category;
    private String coverImg;
    private String seasonTag;
    private String culturalInfo;
    private OriginVO origin;

    @Data
    public static class OriginVO {
        private String province;
        private String city;
        private String county;
        private String address;
        private Double longitude;
        private Double latitude;
    }
}
