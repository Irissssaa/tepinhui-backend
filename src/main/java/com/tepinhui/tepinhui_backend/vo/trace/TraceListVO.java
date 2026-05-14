package com.tepinhui.tepinhui_backend.vo.trace;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(name = "TraceListVO", description = "溯源记录列表项")
public class TraceListVO {

    @Schema(description = "溯源记录ID", example = "1001")
    private Long id;

    @Schema(description = "溯源码，格式：TP-省份缩写-品类缩写-年份-6位随机数", example = "TP-ZJ-LJ-2024-042138")
    private String traceCode;

    @Schema(description = "商品名称", example = "西湖龙井明前茶")
    private String productName;

    @Schema(description = "商家名称", example = "杭州龙井旗舰店")
    private String merchantName;

    @Schema(description = "生产商名称", example = "杭州西湖龙井茶厂")
    private String producerName;

    @Schema(description = "生产日期", example = "2024-03-18")
    private String produceDate;

    @Schema(description = "审核状态：pending/pass/reject", example = "pass")
    private String auditStatus;

    @Schema(description = "创建时间", example = "2024-03-20T10:30:00")
    private LocalDateTime createdAt;
}
