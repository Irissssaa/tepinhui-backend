package com.tepinhui.tepinhui_backend.vo.merchant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(name = "MerchantPageVO", description = "商家分页响应")
public class MerchantPageVO {

    @Schema(description = "商家列表")
    private List<MerchantDetailVO> records;

    @Schema(description = "总数")
    private Long total;

    @Schema(description = "当前页")
    private Long page;

    @Schema(description = "每页大小")
    private Long size;
}
