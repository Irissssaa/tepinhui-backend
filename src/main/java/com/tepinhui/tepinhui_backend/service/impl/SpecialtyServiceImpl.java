package com.tepinhui.tepinhui_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tepinhui.tepinhui_backend.entity.CultureContent;
import com.tepinhui.tepinhui_backend.entity.Origin;
import com.tepinhui.tepinhui_backend.entity.Specialty;
import com.tepinhui.tepinhui_backend.dto.specialty.SpecialtyCreateRequest;
import com.tepinhui.tepinhui_backend.dto.specialty.SpecialtyUpdateRequest;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.mapper.CultureContentMapper;
import com.tepinhui.tepinhui_backend.mapper.OriginMapper;
import com.tepinhui.tepinhui_backend.mapper.SpecialtyMapper;
import com.tepinhui.tepinhui_backend.service.SpecialtyService;
import com.tepinhui.tepinhui_backend.vo.culture.CultureContentVO;
import com.tepinhui.tepinhui_backend.vo.specialty.OriginInfoVO;
import com.tepinhui.tepinhui_backend.vo.specialty.SpecialtyDetailVO;
import com.tepinhui.tepinhui_backend.vo.specialty.SpecialtyListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SpecialtyServiceImpl implements SpecialtyService {

    private final SpecialtyMapper specialtyMapper;
    private final OriginMapper originMapper;
    private final CultureContentMapper cultureContentMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String SPECIALTY_LIST_CACHE = "specialty:list";
    private static final String SPECIALTY_DETAIL_CACHE = "specialty:detail:";
    private static final long CACHE_TTL_HOURS = 2;

    @Override
    public List<SpecialtyListVO> listSpecialties() {
        // 先查 Redis 缓存
        @SuppressWarnings("unchecked")
        List<SpecialtyListVO> cached = (List<SpecialtyListVO>) redisTemplate.opsForValue().get(SPECIALTY_LIST_CACHE);
        if (cached != null) {
            return cached;
        }

        // 查询所有上架特产（is_landing = 1）
        LambdaQueryWrapper<Specialty> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Specialty::getIsLanding, 1);
        wrapper.orderByDesc(Specialty::getCreatedAt);
        List<Specialty> specialties = specialtyMapper.selectList(wrapper);

        List<SpecialtyListVO> result = new ArrayList<>();
        for (Specialty s : specialties) {
            SpecialtyListVO vo = new SpecialtyListVO();
            vo.setId(s.getId());
            vo.setName(s.getName());
            vo.setCategory(s.getCategory());
            vo.setSeasonTag(s.getSeasonTag());
            vo.setCoverImg(s.getCoverImg());
            vo.setIsLanding(s.getIsLanding() != null && s.getIsLanding() == 1);
            result.add(vo);
        }

        // 写入缓存，TTL = 2小时
        redisTemplate.opsForValue().set(SPECIALTY_LIST_CACHE, result, CACHE_TTL_HOURS, TimeUnit.HOURS);

        return result;
    }

    @Override
    public SpecialtyDetailVO getSpecialtyDetail(Long id) {
        String cacheKey = SPECIALTY_DETAIL_CACHE + id;

        // 先查 Redis 缓存
        SpecialtyDetailVO cached = (SpecialtyDetailVO) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 查询特产
        Specialty specialty = specialtyMapper.selectById(id);
        if (specialty == null) {
            throw new BusinessException(404, "特产不存在");
        }

        SpecialtyDetailVO vo = new SpecialtyDetailVO();
        vo.setId(specialty.getId());
        vo.setName(specialty.getName());
        vo.setCategory(specialty.getCategory());
        vo.setCulturalInfo(specialty.getCulturalInfo());
        vo.setSeasonTag(specialty.getSeasonTag());
        vo.setCoverImg(specialty.getCoverImg());
        vo.setIsLanding(specialty.getIsLanding() != null && specialty.getIsLanding() == 1);

        // 联查产地信息
        if (specialty.getOriginId() != null) {
            Origin origin = originMapper.selectById(specialty.getOriginId());
            if (origin != null) {
                OriginInfoVO originVO = new OriginInfoVO();
                originVO.setId(origin.getId());
                originVO.setProvinceName(origin.getProvinceName());
                originVO.setCityName(origin.getCityName());
                originVO.setCountyName(origin.getCountyName());
                originVO.setAddress(origin.getAddress());
                originVO.setLongitude(origin.getLongitude());
                originVO.setLatitude(origin.getLatitude());
                vo.setOrigin(originVO);
            }
        }

        // 联查文化内容列表
        LambdaQueryWrapper<CultureContent> cultureWrapper = new LambdaQueryWrapper<>();
        cultureWrapper.eq(CultureContent::getSpecialtyId, id);
        cultureWrapper.eq(CultureContent::getStatus, 1); // 只查启用状态
        cultureWrapper.orderByAsc(CultureContent::getSortOrder);
        List<CultureContent> cultures = cultureContentMapper.selectList(cultureWrapper);

        List<CultureContentVO> cultureVOList = new ArrayList<>();
        for (CultureContent c : cultures) {
            CultureContentVO cultureVO = new CultureContentVO();
            cultureVO.setId(c.getId());
            cultureVO.setTitle(c.getTitle());
            cultureVO.setContent(c.getContent());
            cultureVO.setType(c.getType());
            cultureVO.setCoverImg(c.getCoverImg());
            cultureVO.setSortOrder(c.getSortOrder());
            cultureVO.setStatus(c.getStatus() != null && c.getStatus() == 1 ? "on" : "off");
            cultureVOList.add(cultureVO);
        }
        vo.setCultureContents(cultureVOList);

        // 写入缓存，TTL = 2小时
        redisTemplate.opsForValue().set(cacheKey, vo, CACHE_TTL_HOURS, TimeUnit.HOURS);

        return vo;
    }

    @Override
    public void createSpecialty(SpecialtyCreateRequest request) {
        throw new BusinessException(501, "新增特产业务逻辑待实现");
    }

    @Override
    public void updateSpecialty(Long id, SpecialtyUpdateRequest request) {
        throw new BusinessException(501, "更新特产业务逻辑待实现");
    }

    @Override
    public void updateSpecialtyStatus(Long id, String status) {
        throw new BusinessException(501, "更新特产状态业务逻辑待实现");
    }
}
