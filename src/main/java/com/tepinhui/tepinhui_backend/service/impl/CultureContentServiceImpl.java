package com.tepinhui.tepinhui_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tepinhui.tepinhui_backend.dto.culture.CultureContentUpdateRequest;
import com.tepinhui.tepinhui_backend.entity.CultureContent;
import com.tepinhui.tepinhui_backend.entity.Specialty;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.mapper.CultureContentMapper;
import com.tepinhui.tepinhui_backend.mapper.SpecialtyMapper;
import com.tepinhui.tepinhui_backend.service.CultureContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CultureContentServiceImpl implements CultureContentService {

    private final SpecialtyMapper specialtyMapper;
    private final CultureContentMapper cultureContentMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SPECIALTY_DETAIL_CACHE = "specialty:detail:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSpecialtyCulture(Long specialtyId, CultureContentUpdateRequest request) {
        // 1. 校验特产存在
        Specialty specialty = specialtyMapper.selectById(specialtyId);
        if (specialty == null) {
            throw new BusinessException(404, "特产不存在");
        }

        // 2. 更新特产文化简介
        if (request.getCulturalInfo() != null) {
            specialty.setCulturalInfo(request.getCulturalInfo());
            specialtyMapper.updateById(specialty);
        }

        // 3. 删除该特产所有旧文化内容
        cultureContentMapper.delete(
            new LambdaQueryWrapper<CultureContent>()
                .eq(CultureContent::getSpecialtyId, specialtyId)
        );

        // 4. 批量插入新文化内容
        List<CultureContentUpdateRequest.CultureContentItemRequest> items = request.getCultureContents();
        if (items != null && !items.isEmpty()) {
            for (CultureContentUpdateRequest.CultureContentItemRequest item : items) {
                CultureContent content = new CultureContent();
                content.setSpecialtyId(specialtyId);
                content.setTitle(item.getTitle());
                content.setContent(item.getContent());
                content.setType(item.getType());
                content.setCoverImg(item.getCoverImg());
                content.setSortOrder(item.getSortOrder());
                // 状态映射：status="on"→1, "off"→0
                if ("on".equals(item.getStatus())) {
                    content.setStatus(1);
                } else {
                    content.setStatus(0);
                }
                cultureContentMapper.insert(content);
            }
        }

        // 5. 清除详情缓存（列表不展示文化内容，不清 list 缓存）
        redisTemplate.delete(SPECIALTY_DETAIL_CACHE + specialtyId);
    }
}
