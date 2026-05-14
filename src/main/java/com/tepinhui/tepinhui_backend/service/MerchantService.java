package com.tepinhui.tepinhui_backend.service;

import com.tepinhui.tepinhui_backend.dto.merchant.MerchantApplyRequest;
import com.tepinhui.tepinhui_backend.dto.merchant.MerchantAuditRequest;
import com.tepinhui.tepinhui_backend.vo.merchant.MerchantDetailVO;
import com.tepinhui.tepinhui_backend.vo.merchant.MerchantPageVO;
import com.tepinhui.tepinhui_backend.vo.merchant.MerchantStatsVO;

public interface MerchantService {

    /**
     * 用户提交商家入驻申请，后续实现应创建 pending 状态 merchant 记录。
     */
    Long apply(MerchantApplyRequest request);

    /**
     * 当前登录用户查看自己的商家资料。
     */
    MerchantDetailVO getCurrentMerchantProfile();

    /**
     * 已审核商家查看经营数据。
     */
    MerchantStatsVO getCurrentMerchantStats();

    /**
     * 管理员分页查询待审核商家。
     */
    MerchantPageVO pagePendingMerchants(Long page, Long size);

    /**
     * 管理员分页查询全部商家。
     */
    MerchantPageVO pageMerchants(Long page, Long size, String status);

    /**
     * 管理员查看商家详情。
     */
    MerchantDetailVO getMerchantDetail(Long id);

    /**
     * 管理员审核商家；通过时后续实现应同步将 user.role 更新为 MERCHANT。
     */
    void auditMerchant(Long id, MerchantAuditRequest request);
}
