package com.tepinhui.tepinhui_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tepinhui.tepinhui_backend.entity.Origin;
import com.tepinhui.tepinhui_backend.entity.Specialty;
import com.tepinhui.tepinhui_backend.mapper.OriginMapper;
import com.tepinhui.tepinhui_backend.mapper.SpecialtyMapper;
import com.tepinhui.tepinhui_backend.service.MapService;
import com.tepinhui.tepinhui_backend.vo.map.SeasonRecommendVO;
import com.tepinhui.tepinhui_backend.vo.map.SpecialtyMapVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MapServiceImpl implements MapService {

    private final SpecialtyMapper specialtyMapper;
    private final OriginMapper originMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /** 缓存 key */
    private static final String SPECIALTY_MAP_CACHE_KEY = "stats:specialty:map";
    private static final String SEASON_RECOMMEND_CACHE_KEY = "stats:season:recommend";
    /** 缓存 TTL */
    private static final Duration SPECIALTY_MAP_TTL = Duration.ofHours(2);
    private static final Duration SEASON_RECOMMEND_TTL = Duration.ofHours(1);

    /** 节气/月份映射表 */
    private static final Map<Integer, String> MONTH_TO_SEASON = Map.ofEntries(
            Map.entry(1, "小寒"), Map.entry(2, "大寒"),
            Map.entry(3, "立春"), Map.entry(4, "清明"), Map.entry(5, "谷雨"),
            Map.entry(6, "立夏"), Map.entry(7, "小暑"), Map.entry(8, "大暑"),
            Map.entry(9, "立秋"), Map.entry(10, "寒露"), Map.entry(11, "立冬"), Map.entry(12, "大雪")
    );

    @Override
    public List<SpecialtyMapVO> listSpecialties() {
        // 1. 查缓存
        Object cached = redisTemplate.opsForValue().get(SPECIALTY_MAP_CACHE_KEY);
        if (cached != null) {
            log.debug("地图特产数据命中缓存");
            return (List<SpecialtyMapVO>) cached;
        }

        // 2. 查数据库：联查 specialty + origin
        List<Specialty> specialties = specialtyMapper.selectList(
                new LambdaQueryWrapper<Specialty>().eq(Specialty::getIsLanding, 1)
        );
        if (CollectionUtils.isEmpty(specialties)) {
            return List.of();
        }

        // 收集 originId 列表
        List<Long> originIds = specialties.stream()
                .map(Specialty::getOriginId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询 origin
        Map<Long, Origin> originMap = new HashMap<>();
        if (!originIds.isEmpty()) {
            originMapper.selectBatchIds(originIds).forEach(o -> originMap.put(o.getId(), o));
        }

        // 按省份分组
        Map<String, List<Specialty>> byProvince = new LinkedHashMap<>();
        for (Specialty s : specialties) {
            Origin origin = originMap.get(s.getOriginId());
            String province = (origin != null && origin.getProvinceName() != null)
                    ? origin.getProvinceName() : "未知";
            byProvince.computeIfAbsent(province, k -> new ArrayList<>()).add(s);
        }

        // 组装 VO（扁平结构：每个特产一行，省份信息放第一行）
        List<SpecialtyMapVO> result = new ArrayList<>();
        for (Map.Entry<String, List<Specialty>> entry : byProvince.entrySet()) {
            String province = entry.getKey();
            List<Specialty> list = entry.getValue();

            // 取第一个特产的产地坐标作为省份坐标
            Origin firstOrigin = originMap.get(list.get(0).getOriginId());
            Double longitude = (firstOrigin != null && firstOrigin.getLongitude() != null)
                    ? firstOrigin.getLongitude().doubleValue() : null;
            Double latitude = (firstOrigin != null && firstOrigin.getLatitude() != null)
                    ? firstOrigin.getLatitude().doubleValue() : null;

            // 每个特产一行VO
            for (Specialty s : list) {
                SpecialtyMapVO vo = new SpecialtyMapVO();
                vo.setRegion(province);
                vo.setSpecialtyName(s.getName());
                vo.setCount(list.size()); // 该省特产数量
                vo.setLongitude(longitude);
                vo.setLatitude(latitude);
                result.add(vo);
            }
        }

        // 写缓存
        redisTemplate.opsForValue().set(SPECIALTY_MAP_CACHE_KEY, result, SPECIALTY_MAP_TTL);
        log.debug("地图特产数据已缓存，共 {} 条记录", result.size());

        return result;
    }

    @Override
    public List<SeasonRecommendVO> listSeasonRecommendations() {
        // 1. 查缓存
        Object cached = redisTemplate.opsForValue().get(SEASON_RECOMMEND_CACHE_KEY);
        if (cached != null) {
            log.debug("节气推荐数据命中缓存");
            return (List<SeasonRecommendVO>) cached;
        }

        int month = LocalDate.now().getMonthValue();
        String currentSeason = MONTH_TO_SEASON.getOrDefault(month, "全年");

        // 查询当季特产
        LambdaQueryWrapper<Specialty> wrapper = new LambdaQueryWrapper<Specialty>()
                .eq(Specialty::getIsLanding, 1);

        List<Specialty> all = specialtyMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(all)) {
            SeasonRecommendVO empty = new SeasonRecommendVO();
            empty.setSeasonTag(currentSeason);
            return List.of(empty);
        }

        // 优先匹配当月节气标签
        List<Specialty> matched = all.stream()
                .filter(s -> s.getSeasonTag() != null && s.getSeasonTag().contains(currentSeason))
                .collect(Collectors.toList());

        // 如果当季没数据，降级为所有上架特产
        if (matched.isEmpty()) {
            matched = all;
        }

        // 最多返回 10 个
        matched = matched.stream().limit(10).collect(Collectors.toList());

        List<SeasonRecommendVO> result = matched.stream()
                .map(s -> {
                    SeasonRecommendVO vo = new SeasonRecommendVO();
                    vo.setId(s.getId());
                    vo.setName(s.getName());
                    vo.setCoverImg(s.getCoverImg());
                    vo.setSeasonTag(s.getSeasonTag());
                    vo.setReason("当前节气：" + currentSeason);
                    return vo;
                })
                .collect(Collectors.toList());

        // 写缓存
        redisTemplate.opsForValue().set(SEASON_RECOMMEND_CACHE_KEY, result, SEASON_RECOMMEND_TTL);
        log.debug("节气推荐数据已缓存，当前节气：{}，推荐 {} 个特产", currentSeason, result.size());

        return result;
    }
}
