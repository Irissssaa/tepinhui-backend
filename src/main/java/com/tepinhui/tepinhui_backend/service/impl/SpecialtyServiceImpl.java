package com.tepinhui.tepinhui_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tepinhui.tepinhui_backend.entity.Origin;
import com.tepinhui.tepinhui_backend.entity.Specialty;
import com.tepinhui.tepinhui_backend.mapper.OriginMapper;
import com.tepinhui.tepinhui_backend.mapper.SpecialtyMapper;
import com.tepinhui.tepinhui_backend.service.SpecialtyService;
import com.tepinhui.tepinhui_backend.vo.specialty.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpecialtyServiceImpl implements SpecialtyService {

    private final SpecialtyMapper specialtyMapper;
    private final OriginMapper originMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String MAP_CACHE_KEY = "specialty:map:all";
    private static final String DETAIL_CACHE_KEY = "specialty:detail:";
    private static final String SEASON_CACHE_KEY = "specialty:season:recommend";

    @Override
    public List<RegionSpecialtyVO> getSpecialtyMap() {
        // 先查 Redis 缓存
        @SuppressWarnings("unchecked")
        List<RegionSpecialtyVO> cached = (List<RegionSpecialtyVO>) redisTemplate.opsForValue().get(MAP_CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        // 查询所有上架特产
        List<Specialty> specialties = specialtyMapper.selectAllLandingSpecialties();

        // 按省份分组
        Map<String, List<Specialty>> byProvince = new LinkedHashMap<>();
        for (Specialty s : specialties) {
            Origin origin = null;
            if (s.getOriginId() != null) {
                origin = originMapper.selectById(s.getOriginId());
            }
            String province = (origin != null && origin.getProvinceName() != null)
                    ? origin.getProvinceName() : "未知";
            byProvince.computeIfAbsent(province, k -> new ArrayList<>()).add(s);
        }

        // 转换为 VO
        List<RegionSpecialtyVO> result = new ArrayList<>();
        for (Map.Entry<String, List<Specialty>> entry : byProvince.entrySet()) {
            RegionSpecialtyVO region = new RegionSpecialtyVO();
            region.setName(entry.getKey());
            region.setCount(entry.getValue().size());
            region.setSpecialties(entry.getValue().stream()
                    .map(this::toSimpleVO)
                    .collect(Collectors.toList()));
            result.add(region);
        }

        // 写入缓存，TTL=2小时
        redisTemplate.opsForValue().set(MAP_CACHE_KEY, result, 2, TimeUnit.HOURS);

        return result;
    }

    @Override
    public SpecialtyVO getSpecialtyDetail(Long id) {
        String cacheKey = DETAIL_CACHE_KEY + id;

        // 先查缓存
        SpecialtyVO cached = (SpecialtyVO) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Specialty specialty = specialtyMapper.selectById(id);
        if (specialty == null) {
            return null;
        }

        SpecialtyVO vo = new SpecialtyVO();
        vo.setId(specialty.getId());
        vo.setName(specialty.getName());
        vo.setCategory(specialty.getCategory());
        vo.setCoverImg(specialty.getCoverImg());
        vo.setSeasonTag(specialty.getSeasonTag());
        vo.setCulturalInfo(specialty.getCulturalInfo());

        // 填充产地信息
        if (specialty.getOriginId() != null) {
            Origin origin = originMapper.selectById(specialty.getOriginId());
            if (origin != null) {
                SpecialtyVO.OriginVO originVO = new SpecialtyVO.OriginVO();
                originVO.setProvince(origin.getProvinceName());
                originVO.setCity(origin.getCityName());
                originVO.setCounty(origin.getCountyName());
                originVO.setAddress(origin.getAddress());
                originVO.setLongitude(origin.getLongitude() != null ? origin.getLongitude().doubleValue() : null);
                originVO.setLatitude(origin.getLatitude() != null ? origin.getLatitude().doubleValue() : null);
                vo.setOrigin(originVO);
            }
        }

        // 缓存 2 小时
        redisTemplate.opsForValue().set(cacheKey, vo, 2, TimeUnit.HOURS);

        return vo;
    }

    @Override
    public SeasonRecommendVO getSeasonRecommend() {
        // 先查缓存
        SeasonRecommendVO cached = (SeasonRecommendVO) redisTemplate.opsForValue().get(SEASON_CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();

        // 根据月份匹配季节标签
        String currentSeason = getSeasonByMonth(month);

        // 查询匹配季节的特产
        LambdaQueryWrapper<Specialty> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Specialty::getIsLanding, 1);
        if (currentSeason != null) {
            wrapper.eq(Specialty::getSeasonTag, currentSeason);
        }
        wrapper.orderByDesc(Specialty::getCreatedAt);
        wrapper.last("LIMIT 10");
        List<Specialty> specialties = specialtyMapper.selectList(wrapper);

        List<SpecialtySimpleVO> recommendations = specialties.stream()
                .map(this::toSimpleVO)
                .collect(Collectors.toList());

        SeasonRecommendVO result = new SeasonRecommendVO();
        result.setCurrentSeason(currentSeason);
        result.setCurrentMonth(month);
        result.setRecommendations(recommendations);

        // 缓存 1 小时
        redisTemplate.opsForValue().set(SEASON_CACHE_KEY, result, 1, TimeUnit.HOURS);

        return result;
    }

    private SpecialtySimpleVO toSimpleVO(Specialty specialty) {
        SpecialtySimpleVO vo = new SpecialtySimpleVO();
        vo.setId(specialty.getId());
        vo.setName(specialty.getName());
        vo.setCoverImg(specialty.getCoverImg());
        vo.setSeasonTag(specialty.getSeasonTag());
        vo.setCategory(specialty.getCategory());
        return vo;
    }

    /**
     * 根据月份返回季节标签
     * 匹配数据库中 season_tag 字段的值
     */
    private String getSeasonByMonth(int month) {
        if (month >= 3 && month <= 5) {
            return "春季";      // 春茶
        } else if (month >= 6 && month <= 8) {
            return "夏季";      // 夏茶
        } else if (month >= 9 && month <= 11) {
            return "秋季";      // 秋茶
        } else {
            return "冬季";      // 冬茶
        }
    }
}
