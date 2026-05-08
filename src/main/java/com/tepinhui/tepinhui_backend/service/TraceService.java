package com.tepinhui.tepinhui_backend.service;

import com.tepinhui.tepinhui_backend.dto.trace.TraceAuditDTO;
import com.tepinhui.tepinhui_backend.dto.trace.TraceInputDTO;
import com.tepinhui.tepinhui_backend.entity.TraceRecord;
import com.tepinhui.tepinhui_backend.vo.trace.TracePageVO;
import com.tepinhui.tepinhui_backend.vo.trace.TraceQueryVO;

public interface TraceService {

    /**
     * 商家录入溯源信息
     * 生成溯源码，状态为 pending
     */
    TraceRecord inputTrace(TraceInputDTO dto, Long merchantId);

    /**
     * 消费者查询溯源全链路
     * 严格按 trace_solution.html 六层结构返回
     * 带 Redis 缓存，TTL=24小时
     */
    TraceQueryVO getTraceInfo(String traceCode);

    /**
     * 获取某商品全部溯源批次列表（公开）
     */
    TracePageVO getTraceListByProduct(Long productId, Long page, Long size);

    /**
     * 管理员：分页查询待审核列表
     */
    TracePageVO getPendingList(Long page, Long size);

    /**
     * 管理员：查看审核详情
     */
    TraceRecord getTraceDetail(Long id);

    /**
     * 管理员：审核操作（通过/驳回）
     * 审核通过时生成二维码URL
     */
    TraceRecord auditTrace(TraceAuditDTO dto);

    /**
     * 管理员：分页查询所有溯源记录
     */
    TracePageVO getAllList(Long page, Long size);
}
