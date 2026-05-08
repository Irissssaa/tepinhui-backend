package com.tepinhui.tepinhui_backend.dto.trace;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(name = "TraceInputDTO", description = "溯源信息录入请求")
public class TraceInputDTO {

    @NotNull(message = "商品ID不能为空")
    @Schema(description = "商品ID")
    private Long productId;

    @Schema(description = "批次号")
    private String batchNo;

    @Schema(description = "产地详细地址")
    private String originAddress;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "生产日期")
    private LocalDate produceDate;

    @Schema(description = "生产商名称")
    private String producerName;

    @Schema(description = "原材料信息")
    private String rawMaterial;

    @Schema(description = "加工厂名称")
    private String processFactory;

    @Schema(description = "加工日期")
    private LocalDate processDate;

    @Schema(description = "加工过程描述")
    private String processDesc;

    @Schema(description = "检测机构")
    private String inspectOrg;

    @Schema(description = "检测日期")
    private LocalDate inspectDate;

    @Schema(description = "检测结果")
    private String inspectResult;

    @Schema(description = "检测报告URL")
    private String inspectReportUrl;

    @Schema(description = "入库时间")
    private LocalDateTime warehouseInTime;

    @Schema(description = "出库时间")
    private LocalDateTime warehouseOutTime;

    @Schema(description = "物流信息")
    private String logisticsInfo;
}
