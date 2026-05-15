package com.tepinhui.tepinhui_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tepinhui.tepinhui_backend.dto.address.AddressCreateRequest;
import com.tepinhui.tepinhui_backend.dto.address.AddressUpdateRequest;
import com.tepinhui.tepinhui_backend.entity.Address;
import com.tepinhui.tepinhui_backend.entity.User;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.mapper.AddressMapper;
import com.tepinhui.tepinhui_backend.mapper.UserMapper;
import com.tepinhui.tepinhui_backend.service.AddressService;
import com.tepinhui.tepinhui_backend.vo.address.AddressVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;
    private final UserMapper userMapper;

    // ==================== 私有工具方法 ====================

    /**
     * 从 SecurityContext 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(401, "未登录");
        }
        String username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof String) {
            username = (String) principal;
        } else if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            throw new BusinessException(401, "登录状态无效");
        }
        if (!StringUtils.hasText(username)) {
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
        return user.getId();
    }

    /**
     * 查询当前用户地址（附所有权校验）
     */
    private Address getAddressWithOwnershipCheck(Long addressId, Long userId) {
        Address address = addressMapper.selectById(addressId);
        if (address == null) {
            throw new BusinessException(404, "收货地址不存在");
        }
        if (!userId.equals(address.getUserId())) {
            throw new BusinessException(403, "无权操作此收货地址");
        }
        return address;
    }

    /**
     * 将 Address 实体转换为 AddressVO
     */
    private AddressVO buildAddressVO(Address address) {
        AddressVO vo = new AddressVO();
        vo.setId(address.getId());
        vo.setUserId(address.getUserId());
        vo.setConsignee(address.getConsignee());
        vo.setPhone(address.getPhone());
        vo.setProvince(address.getProvince());
        vo.setCity(address.getCity());
        vo.setCounty(address.getCounty());
        vo.setDetail(address.getDetail());
        vo.setIsDefault(address.getIsDefault());
        vo.setCreatedAt(address.getCreatedAt());
        vo.setUpdatedAt(address.getUpdatedAt());
        return vo;
    }

    // ==================== 接口实现 ====================

    @Override
    public List<AddressVO> listCurrentUserAddresses() {
        Long userId = getCurrentUserId();
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId)
               .orderByDesc(Address::getIsDefault)   // 默认地址排前面
               .orderByDesc(Address::getCreatedAt); // 再按创建时间倒序
        return addressMapper.selectList(wrapper)
                .stream()
                .map(this::buildAddressVO)
                .toList();
    }

    @Override
    public AddressVO getCurrentUserAddress(Long id) {
        Long userId = getCurrentUserId();
        Address address = getAddressWithOwnershipCheck(id, userId);
        return buildAddressVO(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO createAddress(AddressCreateRequest request) {
        Long userId = getCurrentUserId();

        Address address = new Address();
        address.setUserId(userId);
        address.setConsignee(request.getConsignee());
        address.setPhone(request.getPhone());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setCounty(request.getCounty());
        address.setDetail(request.getDetail());

        // 如果设为默认，先取消该用户所有默认地址
        if (request.getIsDefault() != null && request.getIsDefault() == 1) {
            resetDefaultAddress(userId);
            address.setIsDefault(1);
        } else {
            address.setIsDefault(0);
        }

        addressMapper.insert(address);
        return buildAddressVO(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO updateAddress(Long id, AddressUpdateRequest request) {
        Long userId = getCurrentUserId();
        Address address = getAddressWithOwnershipCheck(id, userId);

        address.setConsignee(request.getConsignee());
        address.setPhone(request.getPhone());
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setCounty(request.getCounty());
        address.setDetail(request.getDetail());

        // 如果设为默认，先取消该用户所有默认地址
        if (request.getIsDefault() != null && request.getIsDefault() == 1) {
            resetDefaultAddress(userId);
        }
        address.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : 0);

        addressMapper.updateById(address);
        return buildAddressVO(address);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAddress(Long id) {
        Long userId = getCurrentUserId();
        Address address = getAddressWithOwnershipCheck(id, userId);
        addressMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultAddress(Long id) {
        Long userId = getCurrentUserId();
        Address address = getAddressWithOwnershipCheck(id, userId);

        // 取消该用户所有默认地址
        resetDefaultAddress(userId);
        // 设置新默认地址
        address.setIsDefault(1);
        addressMapper.updateById(address);
    }

    /**
     * 重置指定用户所有地址为非默认
     */
    private void resetDefaultAddress(Long userId) {
        LambdaUpdateWrapper<Address> wrapper = new LambdaUpdateWrapper<Address>()
            .eq(Address::getUserId, userId)
            .eq(Address::getIsDefault, 1)
            .set(Address::getIsDefault, 0);
        addressMapper.update(null, wrapper);
    }
}
