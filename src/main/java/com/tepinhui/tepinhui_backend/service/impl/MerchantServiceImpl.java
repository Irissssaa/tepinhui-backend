package com.tepinhui.tepinhui_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tepinhui.tepinhui_backend.common.Role;
import com.tepinhui.tepinhui_backend.dto.merchant.MerchantApplyRequest;
import com.tepinhui.tepinhui_backend.dto.merchant.MerchantAuditRequest;
import com.tepinhui.tepinhui_backend.entity.Merchant;
import com.tepinhui.tepinhui_backend.entity.OrderItem;
import com.tepinhui.tepinhui_backend.entity.Product;
import com.tepinhui.tepinhui_backend.entity.User;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.mapper.MerchantMapper;
import com.tepinhui.tepinhui_backend.mapper.OrderItemMapper;
import com.tepinhui.tepinhui_backend.mapper.ProductMapper;
import com.tepinhui.tepinhui_backend.mapper.UserMapper;
import com.tepinhui.tepinhui_backend.service.MerchantService;
import com.tepinhui.tepinhui_backend.vo.merchant.MerchantDetailVO;
import com.tepinhui.tepinhui_backend.vo.merchant.MerchantPageVO;
import com.tepinhui.tepinhui_backend.vo.merchant.MerchantStatsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_APPROVED = "approved";
    private static final String STATUS_REJECTED = "rejected";

    private final MerchantMapper merchantMapper;
    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final OrderItemMapper orderItemMapper;

    // ==================== 商家侧接口实现 ====================

    @Override
    public Long apply(MerchantApplyRequest request) {
        User user = getCurrentUser();

        // ADMIN 和 MERCHANT 角色不能申请入驻
        if (user.getRole() == Role.ADMIN) {
            throw new BusinessException(403, "管理员账户无需申请商家入驻");
        }
        if (user.getRole() == Role.MERCHANT) {
            throw new BusinessException(403, "您已是商家，无需重复申请");
        }

        // 检查是否已有商家申请记录（pending 或 approved）
        Merchant existing = merchantMapper.selectOne(
            new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, user.getId())
                .in(Merchant::getStatus, STATUS_PENDING, STATUS_APPROVED)
                .last("LIMIT 1")
        );
        if (existing != null) {
            throw new BusinessException(400, "您已有入驻申请，无需重复提交");
        }

        // 创建 pending 状态的商家记录
        Merchant merchant = new Merchant();
        merchant.setUserId(user.getId());
        merchant.setShopName(request.getShopName());
        merchant.setLicenseNo(request.getLicenseNo());
        merchant.setQualification(request.getQualification());
        merchant.setStatus(STATUS_PENDING);
        merchantMapper.insert(merchant);

        return merchant.getId();
    }

    @Override
    public MerchantDetailVO getCurrentMerchantProfile() {
        User user = getCurrentUser();
        Merchant merchant = merchantMapper.selectOne(
            new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, user.getId())
                .eq(Merchant::getStatus, STATUS_APPROVED)
                .last("LIMIT 1")
        );
        if (merchant == null) {
            throw new BusinessException(404, "您还不是已审核通过的商家");
        }
        return toDetailVO(merchant);
    }

    @Override
    public MerchantStatsVO getCurrentMerchantStats() {
        User user = getCurrentUser();
        Merchant merchant = merchantMapper.selectOne(
            new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getUserId, user.getId())
                .eq(Merchant::getStatus, STATUS_APPROVED)
                .last("LIMIT 1")
        );
        if (merchant == null) {
            throw new BusinessException(403, "您还不是已审核通过的商家");
        }
        return buildStats(merchant.getId());
    }

    // ==================== 管理端接口实现 ====================

    @Override
    public MerchantPageVO pagePendingMerchants(Long page, Long size) {
        checkAdmin();
        Page<Merchant> pagination = new Page<>(page, size);
        Page<Merchant> result = merchantMapper.selectPage(pagination,
            new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getStatus, STATUS_PENDING)
                .orderByAsc(Merchant::getCreatedAt)
        );
        return toPageVO(result);
    }

    @Override
    public MerchantPageVO pageMerchants(Long page, Long size, String status) {
        checkAdmin();
        Page<Merchant> pagination = new Page<>(page, size);
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Merchant::getStatus, status);
        }
        wrapper.orderByDesc(Merchant::getCreatedAt);
        Page<Merchant> result = merchantMapper.selectPage(pagination, wrapper);
        return toPageVO(result);
    }

    @Override
    public MerchantDetailVO getMerchantDetail(Long id) {
        checkAdmin();
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null) {
            throw new BusinessException(404, "商家不存在");
        }
        return toDetailVO(merchant);
    }

    @Override
    @Transactional
    public void auditMerchant(Long id, MerchantAuditRequest request) {
        checkAdmin();
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null) {
            throw new BusinessException(404, "商家不存在");
        }
        if (!STATUS_PENDING.equals(merchant.getStatus())) {
            throw new BusinessException(400, "该商家不是待审核状态");
        }

        // 更新商家状态
        merchant.setStatus(request.getStatus());
        merchant.setAuditRemark(request.getAuditRemark());
        merchantMapper.updateById(merchant);

        // 如果审核通过，升级用户角色为 MERCHANT
        if (STATUS_APPROVED.equals(request.getStatus())) {
            User user = userMapper.selectById(merchant.getUserId());
            if (user != null && user.getRole() != Role.MERCHANT) {
                user.setRole(Role.MERCHANT);
                userMapper.updateById(user);
            }
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 从 SecurityContext 获取当前登录用户
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "未登录");
        }
        if (!(authentication.getPrincipal() instanceof String username) || !org.springframework.util.StringUtils.hasText(username)) {
            throw new BusinessException(401, "登录状态无效");
        }
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1")
        );
        if (user == null) {
            throw new BusinessException(404, "当前用户不存在");
        }
        return user;
    }

    /**
     * 检查是否为管理员
     */
    private void checkAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "未登录");
        }
        boolean isAdmin = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals);
        if (!isAdmin) {
            throw new BusinessException(403, "权限不足，仅管理员可操作");
        }
    }

    /**
     * 构建商家统计数据
     */
    private MerchantStatsVO buildStats(Long merchantId) {
        MerchantStatsVO stats = new MerchantStatsVO();

        // 商品数量
        Long productCount = productMapper.selectCount(
            new LambdaQueryWrapper<Product>()
                .eq(Product::getMerchantId, merchantId)
        );
        stats.setProductCount(productCount);

        // 订单数量：查询购买过该商家商品的去重订单数
        // 思路：先查出该商家的所有商品ID，再查 order_item 中这些商品的订单ID
        List<Product> merchantProducts = productMapper.selectList(
            new LambdaQueryWrapper<Product>()
                .eq(Product::getMerchantId, merchantId)
                .select(Product::getId)
        );
        if (merchantProducts.isEmpty()) {
            stats.setOrderCount(0L);
            stats.setSalesAmount(BigDecimal.ZERO);
            stats.setViewCount(0L);
            return stats;
        }
        List<Long> productIds = merchantProducts.stream().map(Product::getId).toList();

        // 销售额 + 订单数量（HashSet 去重，避免内存中 stream distinct）
        BigDecimal salesAmount = BigDecimal.ZERO;
        Set<Long> orderIdSet = new HashSet<>();
        List<OrderItem> orderItems = orderItemMapper.selectList(
            new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getProductId, productIds)
                .select(OrderItem::getOrderId, OrderItem::getSubtotal)
        );
        for (OrderItem item : orderItems) {
            orderIdSet.add(item.getOrderId());
            if (item.getSubtotal() != null) {
                salesAmount = salesAmount.add(item.getSubtotal());
            }
        }
        stats.setSalesAmount(salesAmount);
        stats.setOrderCount((long) orderIdSet.size());

        // 浏览量：Product 表暂无 viewCount 字段，先返回 0
        stats.setViewCount(0L);

        return stats;
    }

    /**
     * 将 Merchant 实体转为 MerchantDetailVO
     */
    private MerchantDetailVO toDetailVO(Merchant merchant) {
        MerchantDetailVO vo = new MerchantDetailVO();
        vo.setId(merchant.getId());
        vo.setUserId(merchant.getUserId());
        vo.setShopName(merchant.getShopName());
        vo.setLicenseNo(merchant.getLicenseNo());
        vo.setQualification(merchant.getQualification());
        vo.setStatus(merchant.getStatus());
        vo.setAuditRemark(merchant.getAuditRemark());
        vo.setCreatedAt(merchant.getCreatedAt());
        vo.setUpdatedAt(merchant.getUpdatedAt());
        return vo;
    }

    /**
     * 将分页结果转为 MerchantPageVO
     */
    private MerchantPageVO toPageVO(Page<Merchant> page) {
        MerchantPageVO pageVO = new MerchantPageVO();
        pageVO.setRecords(page.getRecords().stream().map(this::toDetailVO).toList());
        pageVO.setTotal(page.getTotal());
        pageVO.setPage(page.getCurrent());
        pageVO.setSize(page.getSize());
        return pageVO;
    }
}
