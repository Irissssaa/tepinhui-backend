package com.tepinhui.tepinhui_backend.service.impl;

import com.tepinhui.tepinhui_backend.service.AdminStatsService;
import com.tepinhui.tepinhui_backend.vo.admin.AdminStatsVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AdminStatsServiceImpl implements AdminStatsService {

    @Override
    public AdminStatsVO getAdminStats() {
        AdminStatsVO vo = new AdminStatsVO();
        vo.setUserCount(0L);
        vo.setMerchantCount(0L);
        vo.setProductCount(0L);
        vo.setOrderCount(0L);
        vo.setSalesAmount(BigDecimal.ZERO);
        return vo;
    }
}
