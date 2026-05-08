package com.tepinhui.tepinhui_backend.vo.trace;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(name = "TraceListVO", description = "溯源记录列表项")
public class TraceListVO {

    private Long id;
    private String traceCode;
    private String productName;
    private String merchantName;
    private String producerName;
    private String produceDate;
    private String auditStatus;
    private LocalDateTime createdAt;
}
