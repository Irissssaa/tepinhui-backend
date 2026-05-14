package com.tepinhui.tepinhui_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tepinhui.tepinhui_backend.dto.trace.TraceAuditDTO;
import com.tepinhui.tepinhui_backend.dto.trace.TraceInputDTO;
import com.tepinhui.tepinhui_backend.entity.*;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.mapper.MerchantMapper;
import com.tepinhui.tepinhui_backend.mapper.OriginMapper;
import com.tepinhui.tepinhui_backend.mapper.ProductMapper;
import com.tepinhui.tepinhui_backend.mapper.SpecialtyMapper;
import com.tepinhui.tepinhui_backend.mapper.TraceRecordMapper;
import com.tepinhui.tepinhui_backend.service.TraceService;
import com.tepinhui.tepinhui_backend.vo.trace.TraceListVO;
import com.tepinhui.tepinhui_backend.vo.trace.TracePageVO;
import com.tepinhui.tepinhui_backend.vo.trace.TraceQueryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TraceServiceImpl implements TraceService {

    private final TraceRecordMapper traceRecordMapper;
    private final ProductMapper productMapper;
    private final MerchantMapper merchantMapper;
    private final SpecialtyMapper specialtyMapper;
    private final OriginMapper originMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_PREFIX = "trace:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    // ========== 1. 录入溯源 ==========

    @Override
    public TraceRecord inputTrace(TraceInputDTO dto, Long merchantId) {
        // 1. 校验商品存在
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null) throw new BusinessException(404, "商品不存在");

        // 2. 生成溯源码（从商品关联获取省份+品类）
        String traceCode = generateTraceCode(dto.getProductId());

        // 3. 保存记录
        TraceRecord record = new TraceRecord();
        BeanUtils.copyProperties(dto, record);
        record.setTraceCode(traceCode);
        record.setAuditStatus("pending");
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        traceRecordMapper.insert(record);

        return record;
    }

    // ========== 2. 查询溯源（带缓存） ==========

    @Override
    public TraceQueryVO getTraceInfo(String traceCode) {
        String cacheKey = CACHE_KEY_PREFIX + traceCode;

        // 1. 先查 Redis 缓存
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return (TraceQueryVO) cached;
        }

        // 2. 查 MySQL（审核通过的才能查）
        TraceRecord record = traceRecordMapper.selectOne(
            new LambdaQueryWrapper<TraceRecord>()
                .eq(TraceRecord::getTraceCode, traceCode)
                .eq(TraceRecord::getAuditStatus, "pass")
        );

        if (record == null) {
            throw new BusinessException(404, "溯源信息不存在或未审核通过");
        }

        // 3. 联查 product、specialty、merchant、origin
        TraceQueryVO vo = buildTraceVO(record);

        // 4. 写入 Redis，TTL=24小时
        redisTemplate.opsForValue().set(cacheKey, vo, CACHE_TTL);

        return vo;
    }

    // ========== 3. 商品溯源列表（公开） ==========

    @Override
    public TracePageVO getTraceListByProduct(Long productId, Long page, Long size) {
        IPage<TraceRecord> recordPage = traceRecordMapper.selectPage(
            new Page<>(page, size),
            new LambdaQueryWrapper<TraceRecord>()
                .eq(TraceRecord::getProductId, productId)
                .eq(TraceRecord::getAuditStatus, "pass")
                .orderByDesc(TraceRecord::getCreatedAt)
        );

        return buildPageVO(recordPage);
    }

    // ========== 4. 待审核列表（管理员） ==========

    @Override
    public TracePageVO getPendingList(Long page, Long size) {
        IPage<TraceRecord> recordPage = traceRecordMapper.selectPage(
            new Page<>(page, size),
            new LambdaQueryWrapper<TraceRecord>()
                .eq(TraceRecord::getAuditStatus, "pending")
                .orderByAsc(TraceRecord::getCreatedAt)
        );

        return buildPageVO(recordPage);
    }

    // ========== 5. 审核详情（管理员） ==========

    @Override
    public TraceRecord getTraceDetail(Long id) {
        TraceRecord record = traceRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "溯源记录不存在");
        }
        return record;
    }

    // ========== 6. 审核操作 ==========

    @Override
    public TraceRecord auditTrace(TraceAuditDTO dto) {
        TraceRecord record = traceRecordMapper.selectById(dto.getId());
        if (record == null) {
            throw new BusinessException(404, "溯源记录不存在");
        }

        if (!"pass".equals(dto.getStatus()) && !"reject".equals(dto.getStatus())) {
            throw new BusinessException(400, "审核状态只能是 pass 或 reject");
        }

        record.setAuditStatus(dto.getStatus());
        record.setAuditRemark(dto.getRemark());

        if ("pass".equals(dto.getStatus())) {
            // 生成二维码 URL
            String qrUrl = generateQRCodeUrl(record.getTraceCode());
            record.setQrUrl(qrUrl);
        }

        record.setUpdatedAt(LocalDateTime.now());
        traceRecordMapper.updateById(record);

        // 清除缓存
        redisTemplate.delete(CACHE_KEY_PREFIX + record.getTraceCode());

        return record;
    }

    // ========== 7. 所有溯源列表（管理员） ==========

    @Override
    public TracePageVO getAllList(Long page, Long size) {
        IPage<TraceRecord> recordPage = traceRecordMapper.selectPage(
            new Page<>(page, size),
            new LambdaQueryWrapper<TraceRecord>()
                .orderByDesc(TraceRecord::getCreatedAt)
        );

        return buildPageVO(recordPage);
    }

    // ========== 工具方法 ==========

    /**
     * 生成溯源码：TP-{省份缩写}-{品类缩写}-{年份}-{6位随机数}
     * 省份和品类从 product → specialty → origin 获取
     */
    private String generateTraceCode(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) throw new BusinessException(404, "商品不存在");

        Specialty specialty = specialtyMapper.selectById(product.getSpecialtyId());
        if (specialty == null) throw new BusinessException(404, "商品未关联特产");

        Origin origin = originMapper.selectById(specialty.getOriginId());
        if (origin == null) throw new BusinessException(404, "特产未关联产地");

        // 省份缩写：取前2字首字母（无pinyin依赖，用前2字拼音首字母近似）
        String provinceAbbr = toPinyinAbbr(origin.getProvinceName());
        String categoryAbbr = toPinyinAbbr(specialty.getCategory());
        String year = String.valueOf(LocalDate.now().getYear());
        String random = String.format("%06d", new Random().nextInt(999999));

        return String.format("TP-%s-%s-%s-%s", provinceAbbr, categoryAbbr, year, random);
    }

    /** 汉字转拼音缩写（首字母，大写），无法转换时用前2字首字符替代 */
    private String toPinyinAbbr(String cn) {
        if (cn == null || cn.isEmpty()) return "XX";
        // 常用省份/品类首字母近似
        // 全小写时直接取首字拼音首字母的缩写
        // 简化：取前2个汉字的首字母，无法识别返回首2字符
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cn.length() && i < 2; i++) {
            char c = cn.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FA5) {
                // 常用汉字到拼音首字母的映射（覆盖常见特产省份和品类）
                String py = PINYIN_MAP.get(c);
                sb.append(py != null ? py : Character.toUpperCase(c));
            } else {
                sb.append(Character.toUpperCase(c));
            }
        }
        return sb.length() >= 2 ? sb.toString() : "XX";
    }

    private static final Map<Character, String> PINYIN_MAP = Map.ofEntries(
        // 省份
        Map.entry('浙', "ZJ"), Map.entry('江', "J"),
        Map.entry('福', "FJ"), Map.entry('建', "J"),
        Map.entry('四', "SC"), Map.entry('川', "C"),
        Map.entry('安', "AH"), Map.entry('徽', "H"),
        Map.entry('湖', "HUN"), Map.entry('南', "N"),
        Map.entry('山', "SD"), Map.entry('东', "D"),
        Map.entry('河', "HEN"),
        Map.entry('广', "GD"),
        Map.entry('云', "YN"),
        Map.entry('贵', "GZ"), Map.entry('州', "Z"),
        Map.entry('陕', "SX"),
        Map.entry('甘', "GS"), Map.entry('肃', "S"),
        Map.entry('新', "XJ"), Map.entry('疆', "J"),
        Map.entry('青', "QH"), Map.entry('海', "H"),
        Map.entry('宁', "NX"), Map.entry('夏', "X"),
        Map.entry('西', "XZ"), Map.entry('藏', "Z"),
        Map.entry('内', "NM"), Map.entry('蒙', "M"), Map.entry('古', "G"),
        Map.entry('吉', "JL"), Map.entry('林', "L"),
        Map.entry('黑', "HLJ"), Map.entry('龙', "L"),
        Map.entry('辽', "LN"),
        // 常见品类
        Map.entry('苹', "PG"), Map.entry('果', "G"),
        Map.entry('葡', "PT"), Map.entry('萄', "T"),
        Map.entry('柑', "G"), Map.entry('橘', "J"),
        Map.entry('梨', "L"), Map.entry('枣', "Z"),
        Map.entry('核', "H"), Map.entry('桃', "T"), Map.entry('仁', "R"),
        Map.entry('枸', "GOU"), Map.entry('杞', "Q"),
        Map.entry('芝', "ZM"), Map.entry('麻', "M"),
        Map.entry('大', "D"), Map.entry('蒜', "S"),
        Map.entry('辣', "L"), Map.entry('椒', "J"),
        Map.entry('蜂', "FM"), Map.entry('蜜', "M"),
        Map.entry('米', "M"), Map.entry('酒', "J"),
        Map.entry('醋', "C"), Map.entry('白', "B")
    );

    /**
     * 生成二维码 URL（二维码生成服务）
     */
    private String generateQRCodeUrl(String traceCode) {
        // TODO: 调用二维码生成服务（存 OSS），返回 URL
        return "https://oss.example.com/qr/trace/" + traceCode + ".png";
    }

    /**
     * 构建六层嵌套 VO（严格按 trace_solution.html）
     */
    private TraceQueryVO buildTraceVO(TraceRecord record) {
        TraceQueryVO vo = new TraceQueryVO();
        vo.setTraceCode(record.getTraceCode());
        vo.setBatchNo(record.getBatchNo());

        // product 层（从 DB 联查）
        Product product = productMapper.selectById(record.getProductId());
        Merchant merchant = product != null ? merchantMapper.selectById(product.getMerchantId()) : null;
        TraceQueryVO.ProductVO productVO = new TraceQueryVO.ProductVO();
        productVO.setName(product != null ? product.getName() : null);
        productVO.setSpec(product != null ? product.getDescription() : null);
        productVO.setMerchantName(merchant != null ? merchant.getShopName() : null);
        // 取 images 第一个作为封面
        String coverImg = null;
        if (product != null && product.getImages() != null && !product.getImages().isEmpty()) {
            coverImg = product.getImages().split(",")[0].trim();
        }
        productVO.setCoverImg(coverImg);
        vo.setProduct(productVO);

        // origin 层
        TraceQueryVO.OriginVO originVO = new TraceQueryVO.OriginVO();
        originVO.setProvince("浙江");
        originVO.setCity("杭州");
        originVO.setCounty("西湖区");
        originVO.setAddress(record.getOriginAddress());
        originVO.setLongitude(record.getLongitude());
        originVO.setLatitude(record.getLatitude());
        vo.setOrigin(originVO);

        // production 层
        TraceQueryVO.ProductionVO productionVO = new TraceQueryVO.ProductionVO();
        productionVO.setProduceDate(formatDate(record.getProduceDate()));
        productionVO.setProducer(record.getProducerName());
        productionVO.setRawMaterial(record.getRawMaterial());
        vo.setProduction(productionVO);

        // process 层
        TraceQueryVO.ProcessVO processVO = new TraceQueryVO.ProcessVO();
        processVO.setFactory(record.getProcessFactory());
        processVO.setProcessDate(formatDate(record.getProcessDate()));
        processVO.setProcessDesc(record.getProcessDesc());
        vo.setProcess(processVO);

        // inspection 层
        TraceQueryVO.InspectionVO inspectionVO = new TraceQueryVO.InspectionVO();
        inspectionVO.setOrg(record.getInspectOrg());
        inspectionVO.setInspectDate(formatDate(record.getInspectDate()));
        inspectionVO.setResult(record.getInspectResult());
        inspectionVO.setReportUrl(record.getInspectReportUrl());
        vo.setInspection(inspectionVO);

        // logistics 层
        TraceQueryVO.LogisticsVO logisticsVO = new TraceQueryVO.LogisticsVO();
        logisticsVO.setWarehouseIn(formatDateTime(record.getWarehouseInTime()));
        logisticsVO.setWarehouseOut(formatDateTime(record.getWarehouseOutTime()));
        logisticsVO.setLogisticsInfo(record.getLogisticsInfo());
        vo.setLogistics(logisticsVO);

        return vo;
    }

    private TracePageVO buildPageVO(IPage<TraceRecord> page) {
        TracePageVO vo = new TracePageVO();
        List<TraceListVO> list = page.getRecords().stream().map(record -> {
            TraceListVO item = new TraceListVO();
            item.setId(record.getId());
            item.setTraceCode(record.getTraceCode());
            // 联查 productName, merchantName
            Product product = productMapper.selectById(record.getProductId());
            Merchant merchant = product != null ? merchantMapper.selectById(product.getMerchantId()) : null;
            item.setProductName(product != null ? product.getName() : null);
            item.setMerchantName(merchant != null ? merchant.getShopName() : null);
            item.setProducerName(record.getProducerName());
            item.setProduceDate(formatDate(record.getProduceDate()));
            item.setAuditStatus(record.getAuditStatus());
            item.setCreatedAt(record.getCreatedAt());
            return item;
        }).collect(Collectors.toList());

        vo.setRecords(list);
        vo.setTotal(page.getTotal());
        vo.setPage(page.getCurrent());
        vo.setSize(page.getSize());
        return vo;
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
