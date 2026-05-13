package com.tepinhui.tepinhui_backend.service;

import com.tepinhui.tepinhui_backend.dto.address.AddressCreateRequest;
import com.tepinhui.tepinhui_backend.dto.address.AddressUpdateRequest;
import com.tepinhui.tepinhui_backend.vo.address.AddressVO;

import java.util.List;

public interface AddressService {

    List<AddressVO> listCurrentUserAddresses();

    AddressVO getCurrentUserAddress(Long id);

    AddressVO createAddress(AddressCreateRequest request);

    AddressVO updateAddress(Long id, AddressUpdateRequest request);

    void deleteAddress(Long id);

    void setDefaultAddress(Long id);
}
