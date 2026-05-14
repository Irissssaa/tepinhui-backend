package com.tepinhui.tepinhui_backend.service.impl;

import com.tepinhui.tepinhui_backend.dto.address.AddressCreateRequest;
import com.tepinhui.tepinhui_backend.dto.address.AddressUpdateRequest;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.service.AddressService;
import com.tepinhui.tepinhui_backend.vo.address.AddressVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    @Override
    public List<AddressVO> listCurrentUserAddresses() {
        return List.of();
    }

    @Override
    public AddressVO getCurrentUserAddress(Long id) {
        throw new BusinessException(501, "收货地址详情业务逻辑待实现");
    }

    @Override
    public AddressVO createAddress(AddressCreateRequest request) {
        throw new BusinessException(501, "新增收货地址业务逻辑待实现");
    }

    @Override
    public AddressVO updateAddress(Long id, AddressUpdateRequest request) {
        throw new BusinessException(501, "更新收货地址业务逻辑待实现");
    }

    @Override
    public void deleteAddress(Long id) {
        throw new BusinessException(501, "删除收货地址业务逻辑待实现");
    }

    @Override
    public void setDefaultAddress(Long id) {
        throw new BusinessException(501, "设置默认收货地址业务逻辑待实现");
    }
}
