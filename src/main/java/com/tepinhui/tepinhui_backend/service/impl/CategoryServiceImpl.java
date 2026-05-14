package com.tepinhui.tepinhui_backend.service.impl;

import com.tepinhui.tepinhui_backend.service.CategoryService;
import com.tepinhui.tepinhui_backend.vo.category.CategoryVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Override
    public List<CategoryVO> listCategories() {
        return List.of();
    }
}
