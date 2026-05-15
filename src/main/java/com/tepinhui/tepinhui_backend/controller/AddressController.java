package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.dto.address.AddressCreateRequest;
import com.tepinhui.tepinhui_backend.dto.address.AddressUpdateRequest;
import com.tepinhui.tepinhui_backend.service.AddressService;
import com.tepinhui.tepinhui_backend.vo.address.AddressVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "收货地址-用户侧接口", description = "当前登录用户的收货地址管理接口")
@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(
        summary = "收货地址列表",
        description = "查询当前登录用户的收货地址列表，按默认优先+创建时间倒序排列"
    )
    public Result<List<AddressVO>> listAddresses() {
        return Result.success(addressService.listCurrentUserAddresses());
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "收货地址详情",
        description = "根据地址ID查询当前登录用户的收货地址详情；返回403表示无权访问"
    )
    public Result<AddressVO> getAddress(
        @Parameter(description = "地址ID", required = true)
        @PathVariable Long id
    ) {
        return Result.success(addressService.getCurrentUserAddress(id));
    }

    @PostMapping
    @Operation(
        summary = "新增收货地址",
        description = "为当前登录用户新增收货地址；如设isDefault=1会自动取消其他默认地址"
    )
    public Result<AddressVO> createAddress(
        @Parameter(description = "收货地址创建请求", required = true)
        @RequestBody @Valid AddressCreateRequest request
    ) {
        return Result.success("收货地址创建成功", addressService.createAddress(request));
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "更新收货地址",
        description = "根据地址ID更新当前登录用户的收货地址；返回403表示无权操作"
    )
    public Result<AddressVO> updateAddress(
        @Parameter(description = "地址ID", required = true)
        @PathVariable Long id,
        @Parameter(description = "收货地址更新请求", required = true)
        @RequestBody @Valid AddressUpdateRequest request
    ) {
        return Result.success("收货地址更新成功", addressService.updateAddress(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除收货地址",
        description = "根据地址ID删除当前登录用户的收货地址；返回403表示无权操作"
    )
    public Result<Void> deleteAddress(
        @Parameter(description = "地址ID", required = true)
        @PathVariable Long id
    ) {
        addressService.deleteAddress(id);
        return Result.success("收货地址删除成功", null);
    }

    @PutMapping("/{id}/default")
    @Operation(
        summary = "设置默认收货地址",
        description = "将当前登录用户的指定地址设为默认地址；返回403表示无权操作"
    )
    public Result<Void> setDefaultAddress(
        @Parameter(description = "地址ID", required = true)
        @PathVariable Long id
    ) {
        addressService.setDefaultAddress(id);
        return Result.success("默认收货地址设置成功", null);
    }
}
