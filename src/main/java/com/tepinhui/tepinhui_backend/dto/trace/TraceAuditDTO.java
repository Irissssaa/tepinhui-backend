package com.tepinhui.tepinhui_backend.dto.trace;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(name = "TraceAuditDTO", description = "溯源审核请求")
public class TraceAuditDTO {

    @NotNull(message = "溯源记录ID不能为空")
    @Schema(description = "溯源记录ID")
    private Long id;

    @NotBlank(message = "审核状态不能为空")
    @Schema(description = "审核状态：pass=通过，reject=驳回")
    private String status;

    @Schema(description = "审核备注")
    private String remark;
}
