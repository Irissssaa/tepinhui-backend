package com.tepinhui.tepinhui_backend.vo.trace;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(name = "TraceQueryVO", description = "溯源查询响应（六层嵌套结构）")
public class TraceQueryVO {

    @Schema(description = "溯源码")
    private String traceCode;

    @Schema(description = "批次号")
    private String batchNo;

    /** ---------- product ---------- */
    @Schema(description = "商品信息")
    private ProductVO product;

    /** ---------- origin ---------- */
    @Schema(description = "产地信息")
    private OriginVO origin;

    /** ---------- production ---------- */
    @Schema(description = "生产信息")
    private ProductionVO production;

    /** ---------- process ---------- */
    @Schema(description = "加工信息")
    private ProcessVO process;

    /** ---------- inspection ---------- */
    @Schema(description = "质检信息")
    private InspectionVO inspection;

    /** ---------- logistics ---------- */
    @Schema(description = "物流信息")
    private LogisticsVO logistics;

    // ---------- 嵌套内部类 ----------

    @Data
    public static class ProductVO {
        private String name;
        private String spec;
        private String merchantName;
        private String coverImg;
    }

    @Data
    public static class OriginVO {
        private String province;
        private String city;
        private String county;
        private String address;
        private BigDecimal longitude;
        private BigDecimal latitude;
    }

    @Data
    public static class ProductionVO {
        private String produceDate;
        private String producer;
        private String rawMaterial;
    }

    @Data
    public static class ProcessVO {
        private String factory;
        private String processDate;
        private String processDesc;
    }

    @Data
    public static class InspectionVO {
        private String org;
        private String inspectDate;
        private String result;
        private String reportUrl;
    }

    @Data
    public static class LogisticsVO {
        private String warehouseIn;
        private String warehouseOut;
        private String logisticsInfo;
    }
}
