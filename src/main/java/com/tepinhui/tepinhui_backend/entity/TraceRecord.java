package com.tepinhui.tepinhui_backend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("trace_record")
public class TraceRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 溯源码（UNIQUE） */
    private String traceCode;

    /** 关联商品ID */
    private Long productId;

    /** 批次号 */
    private String batchNo;

    /** 二维码URL（审核通过后生成） */
    private String qrUrl;

    /** 产地详细地址 */
    private String originAddress;

    /** 经度 */
    private BigDecimal longitude;

    /** 纬度 */
    private BigDecimal latitude;

    /** 生产日期 */
    private LocalDate produceDate;

    /** 生产商名称 */
    private String producerName;

    /** 原材料信息 */
    private String rawMaterial;

    /** 加工厂名称 */
    private String processFactory;

    /** 加工日期 */
    private LocalDate processDate;

    /** 加工过程描述 */
    private String processDesc;

    /** 检测机构 */
    private String inspectOrg;

    /** 检测日期 */
    private LocalDate inspectDate;

    /** 检测结果 */
    private String inspectResult;

    /** 检测报告URL */
    private String inspectReportUrl;

    /** 入库时间 */
    private LocalDateTime warehouseInTime;

    /** 出库时间 */
    private LocalDateTime warehouseOutTime;

    /** 物流信息 */
    private String logisticsInfo;

    /** 审核状态：pending/pass/reject */
    private String auditStatus;

    /** 审核备注 */
    private String auditRemark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
