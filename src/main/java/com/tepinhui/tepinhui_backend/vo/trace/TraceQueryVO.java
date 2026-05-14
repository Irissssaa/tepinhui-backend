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
    @Schema(name = "TraceProductVO", description = "溯源链路中的商品信息")
    public static class ProductVO {
        @Schema(description = "商品名称", example = "西湖龙井明前茶")
        private String name;

        @Schema(description = "商品规格", example = "250g/盒")
        private String spec;

        @Schema(description = "商家名称", example = "杭州龙井旗舰店")
        private String merchantName;

        @Schema(description = "商品封面图 URL", example = "https://oss.example.com/product/1001-cover.jpg")
        private String coverImg;
    }

    @Data
    @Schema(name = "TraceOriginVO", description = "溯源链路中的产地信息")
    public static class OriginVO {
        @Schema(description = "产地省份", example = "浙江省")
        private String province;

        @Schema(description = "产地城市", example = "杭州市")
        private String city;

        @Schema(description = "产地区县", example = "西湖区")
        private String county;

        @Schema(description = "产地详细地址", example = "龙井村狮峰山茶园")
        private String address;

        @Schema(description = "经度", example = "120.101430")
        private BigDecimal longitude;

        @Schema(description = "纬度", example = "30.229110")
        private BigDecimal latitude;
    }

    @Data
    @Schema(name = "TraceProductionVO", description = "溯源链路中的生产信息")
    public static class ProductionVO {
        @Schema(description = "生产日期", example = "2024-03-18")
        private String produceDate;

        @Schema(description = "生产商名称", example = "杭州西湖龙井茶厂")
        private String producer;

        @Schema(description = "原材料信息", example = "西湖龙井一芽一叶鲜叶")
        private String rawMaterial;
    }

    @Data
    @Schema(name = "TraceProcessVO", description = "溯源链路中的加工信息")
    public static class ProcessVO {
        @Schema(description = "加工厂名称", example = "杭州西湖龙井精制中心")
        private String factory;

        @Schema(description = "加工日期", example = "2024-03-19")
        private String processDate;

        @Schema(description = "加工过程描述", example = "完成摊青、杀青、回潮和辉锅工序")
        private String processDesc;
    }

    @Data
    @Schema(name = "TraceInspectionVO", description = "溯源链路中的质检信息")
    public static class InspectionVO {
        @Schema(description = "检测机构", example = "浙江省农产品质量检测中心")
        private String org;

        @Schema(description = "检测日期", example = "2024-03-20")
        private String inspectDate;

        @Schema(description = "检测结果", example = "合格")
        private String result;

        @Schema(description = "检测报告 URL", example = "https://oss.example.com/reports/trace-1001.pdf")
        private String reportUrl;
    }

    @Data
    @Schema(name = "TraceLogisticsVO", description = "溯源链路中的物流信息")
    public static class LogisticsVO {
        @Schema(description = "入库时间", example = "2024-03-21 09:00:00")
        private String warehouseIn;

        @Schema(description = "出库时间", example = "2024-03-22 14:00:00")
        private String warehouseOut;

        @Schema(description = "物流信息 JSON 字符串或文本描述", example = "{\"company\":\"顺丰\",\"trackingNo\":\"SF1234567890\"}")
        private String logisticsInfo;
    }
}
