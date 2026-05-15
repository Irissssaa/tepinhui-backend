package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.common.Result;
import com.tepinhui.tepinhui_backend.service.CategoryService;
import com.tepinhui.tepinhui_backend.vo.category.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "分类-公开接口", description = "公开分类列表查询接口")
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(
        summary = "分类列表",
        description = "查询公开分类列表，支持一级/二级分类树形结构，按 sortOrder 排序"
    )
    public Result<List<CategoryVO>> listCategories() {
        return Result.success(categoryService.listCategories());
    }
}
