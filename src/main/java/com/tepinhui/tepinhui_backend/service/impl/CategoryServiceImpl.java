package com.tepinhui.tepinhui_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tepinhui.tepinhui_backend.entity.Category;
import com.tepinhui.tepinhui_backend.mapper.CategoryMapper;
import com.tepinhui.tepinhui_backend.service.CategoryService;
import com.tepinhui.tepinhui_backend.vo.category.CategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryVO> listCategories() {
        // 查询已启用的分类，按 sortOrder 排序
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getStatus, 1)
               .orderByAsc(Category::getParentId)
               .orderByAsc(Category::getSortOrder);

        List<Category> categories = categoryMapper.selectList(wrapper);
        return categories.stream().map(this::buildCategoryVO).toList();
    }

    private CategoryVO buildCategoryVO(Category category) {
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setParentId(category.getParentId());
        vo.setName(category.getName());
        vo.setIcon(category.getIcon());
        vo.setSortOrder(category.getSortOrder());
        // 状态映射：1 → "on"，0 → "off"
        vo.setStatus(category.getStatus() != null && category.getStatus() == 1 ? "on" : "off");
        return vo;
    }
}
