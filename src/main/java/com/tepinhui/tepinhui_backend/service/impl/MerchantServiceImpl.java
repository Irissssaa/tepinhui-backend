package com.tepinhui.tepinhui_backend.service.impl;

import com.tepinhui.tepinhui_backend.dto.merchant.MerchantApplyRequest;
import com.tepinhui.tepinhui_backend.dto.merchant.MerchantAuditRequest;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.service.MerchantService;
import com.tepinhui.tepinhui_backend.vo.merchant.MerchantDetailVO;
import com.tepinhui.tepinhui_backend.vo.merchant.MerchantPageVO;
import com.tepinhui.tepinhui_backend.vo.merchant.MerchantStatsVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class MerchantServiceImpl implements MerchantService {

    @Override
    public Long apply(MerchantApplyRequest request) {
        throw new BusinessException(501, "商家入驻申请业务逻辑待实现");
    }

    @Override
    public MerchantDetailVO getCurrentMerchantProfile() {
        throw new BusinessException(501, "当前商家资料业务逻辑待实现");
    }

    @Override
    public MerchantStatsVO getCurrentMerchantStats() {
        MerchantStatsVO stats = new MerchantStatsVO();
        stats.setProductCount(0L);
        stats.setOrderCount(0L);
        stats.setSalesAmount(BigDecimal.ZERO);
        stats.setViewCount(0L);
        return stats;
    }

    @Override
    public MerchantPageVO pagePendingMerchants(Long page, Long size) {
        return emptyPage(page, size);
    }

    @Override
    public MerchantPageVO pageMerchants(Long page, Long size, String status) {
        return emptyPage(page, size);
    }

    @Override
    public MerchantDetailVO getMerchantDetail(Long id) {
        throw new BusinessException(501, "商家详情业务逻辑待实现");
    }

    @Override
    public void auditMerchant(Long id, MerchantAuditRequest request) {
        throw new BusinessException(501, "商家审核业务逻辑待实现");
    }

    private MerchantPageVO emptyPage(Long page, Long size) {
        MerchantPageVO pageVO = new MerchantPageVO();
        pageVO.setRecords(List.of());
        pageVO.setTotal(0L);
        pageVO.setPage(page);
        pageVO.setSize(size);
        return pageVO;
    }
}
