package com.tepinhui.tepinhui_backend.service.impl;

import com.tepinhui.tepinhui_backend.dto.admin.AdminLogQueryRequest;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.service.AdminLogService;
import com.tepinhui.tepinhui_backend.vo.admin.AdminLogAppliedFiltersVO;
import com.tepinhui.tepinhui_backend.vo.admin.AdminLogPageVO;
import com.tepinhui.tepinhui_backend.vo.admin.AdminLogRecordVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AdminLogServiceImpl implements AdminLogService {

    private static final int BAD_REQUEST_CODE = 400;
    private static final int INTERNAL_ERROR_CODE = 500;
    private static final Pattern LOG_LINE_PATTERN = Pattern.compile(
            "^(?<timestamp>\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) \\|\\s*(?<level>TRACE|DEBUG|INFO|WARN|ERROR)\\s*\\| \\[(?<thread>[^\\]]*)] \\| (?<logger>.*?) \\| (?<message>.*)$");
    private static final Pattern TIMESTAMP_PREFIX_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2} .*");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Set<String> ALLOWED_LEVELS = Set.of("TRACE", "DEBUG", "INFO", "WARN", "ERROR");

    private final String logFilePath;
    private final int defaultLimit;
    private final int maxLimit;

    public AdminLogServiceImpl(
            @Value("${app.log.file-path}") String logFilePath,
            @Value("${app.log.read.default-limit}") int defaultLimit,
            @Value("${app.log.read.max-limit}") int maxLimit) {
        this.logFilePath = logFilePath;
        this.defaultLimit = defaultLimit;
        this.maxLimit = maxLimit;
    }

    @Override
    public AdminLogPageVO getLogs(AdminLogQueryRequest queryRequest) {
        validateConfiguration();
        NormalizedQuery normalizedQuery = normalizeQuery(queryRequest);
        Path targetPath = resolveReadableLogFile();
        List<AdminLogRecordVO> records = readLatestMatchingRecords(targetPath, normalizedQuery);

        AdminLogPageVO pageVO = new AdminLogPageVO();
        pageVO.setAppliedFilters(toAppliedFilters(normalizedQuery));
        pageVO.setLogFilePath(logFilePath);
        pageVO.setReturnedCount(records.size());
        pageVO.setRecords(records);
        return pageVO;
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(logFilePath)) {
            throw new BusinessException(INTERNAL_ERROR_CODE, "日志文件路径配置缺失");
        }
        if (defaultLimit < 1) {
            throw new BusinessException(INTERNAL_ERROR_CODE, "日志默认读取条数配置非法");
        }
        if (maxLimit < 1 || defaultLimit > maxLimit) {
            throw new BusinessException(INTERNAL_ERROR_CODE, "日志读取上限配置非法");
        }
    }

    private NormalizedQuery normalizeQuery(AdminLogQueryRequest queryRequest) {
        AdminLogQueryRequest request = queryRequest == null ? new AdminLogQueryRequest() : queryRequest;
        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime();
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw new BusinessException(BAD_REQUEST_CODE, "startTime 不能晚于 endTime");
        }

        String level = normalizeLevel(request.getLevel());
        String keyword = StringUtils.hasText(request.getKeyword()) ? request.getKeyword() : null;
        int limit = request.getLimit() == null ? defaultLimit : request.getLimit();
        if (limit < 1 || limit > maxLimit) {
            throw new BusinessException(BAD_REQUEST_CODE, "limit 必须在 1 到 " + maxLimit + " 之间");
        }

        return new NormalizedQuery(startTime, endTime, level, keyword, limit);
    }

    private String normalizeLevel(String level) {
        if (!StringUtils.hasText(level)) {
            return null;
        }
        String normalized = level.trim();
        if (!ALLOWED_LEVELS.contains(normalized)) {
            throw new BusinessException(BAD_REQUEST_CODE, "level 仅支持 TRACE、DEBUG、INFO、WARN、ERROR");
        }
        return normalized;
    }

    private Path resolveReadableLogFile() {
        Path path = Path.of(logFilePath).normalize();
        if (!Files.exists(path)) {
            throw new BusinessException(INTERNAL_ERROR_CODE, "日志文件不存在");
        }
        if (!Files.isRegularFile(path)) {
            throw new BusinessException(INTERNAL_ERROR_CODE, "日志文件路径不是普通文件");
        }
        if (!Files.isReadable(path)) {
            throw new BusinessException(INTERNAL_ERROR_CODE, "日志文件无读取权限");
        }
        return path;
    }

    private List<AdminLogRecordVO> readLatestMatchingRecords(Path path, NormalizedQuery query) {
        List<AdminLogRecordVO> records = new ArrayList<>(query.limit());
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(path.toFile(), "r")) {
            long pointer = randomAccessFile.length() - 1;
            if (pointer < 0) {
                return records;
            }

            ByteArrayOutputStream reversedLineBuffer = new ByteArrayOutputStream(256);
            while (pointer >= 0 && records.size() < query.limit()) {
                randomAccessFile.seek(pointer);
                int currentByte = randomAccessFile.read();
                if (currentByte == '\n') {
                    appendIfMatched(reversedLineBuffer, records, query);
                    reversedLineBuffer.reset();
                } else if (currentByte != '\r') {
                    reversedLineBuffer.write(currentByte);
                }
                pointer--;
            }

            appendIfMatched(reversedLineBuffer, records, query);
            return records;
        } catch (FileNotFoundException e) {
            throw new BusinessException(INTERNAL_ERROR_CODE, "日志文件不存在或无读取权限");
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("读取日志文件失败: {}", path, e);
            throw new BusinessException(INTERNAL_ERROR_CODE, "读取日志文件失败");
        }
    }

    private void appendIfMatched(ByteArrayOutputStream reversedLineBuffer,
                                 List<AdminLogRecordVO> records,
                                 NormalizedQuery query) {
        if (reversedLineBuffer.size() == 0 || records.size() >= query.limit()) {
            return;
        }

        String rawLine = decodeReversedLine(reversedLineBuffer);
        if (!StringUtils.hasText(rawLine)) {
            return;
        }

        Matcher matcher = LOG_LINE_PATTERN.matcher(rawLine);
        if (!matcher.matches()) {
            if (TIMESTAMP_PREFIX_PATTERN.matcher(rawLine).matches()) {
                throw new BusinessException(INTERNAL_ERROR_CODE, "日志文件格式异常，存在无法解析的日志主行");
            }
            return;
        }

        AdminLogRecordVO record = toRecord(matcher, rawLine);
        if (matchesFilters(record, query)) {
            records.add(record);
        }
    }

    private String decodeReversedLine(ByteArrayOutputStream reversedLineBuffer) {
        byte[] bytes = reversedLineBuffer.toByteArray();
        for (int left = 0, right = bytes.length - 1; left < right; left++, right--) {
            byte temp = bytes[left];
            bytes[left] = bytes[right];
            bytes[right] = temp;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private AdminLogRecordVO toRecord(Matcher matcher, String rawLine) {
        try {
            LocalDateTime timestamp = LocalDateTime.parse(matcher.group("timestamp"), TIMESTAMP_FORMATTER);
            AdminLogRecordVO record = new AdminLogRecordVO();
            record.setTimestamp(timestamp);
            record.setLevel(matcher.group("level"));
            record.setMessage(matcher.group("message"));
            record.setRawLine(rawLine);
            return record;
        } catch (DateTimeParseException e) {
            throw new BusinessException(INTERNAL_ERROR_CODE, "日志文件格式异常，日志时间无法解析");
        }
    }

    private boolean matchesFilters(AdminLogRecordVO record, NormalizedQuery query) {
        if (query.startTime() != null && record.getTimestamp().isBefore(query.startTime())) {
            return false;
        }
        if (query.endTime() != null && record.getTimestamp().isAfter(query.endTime())) {
            return false;
        }
        if (query.level() != null && !query.level().equals(record.getLevel())) {
            return false;
        }
        return query.keyword() == null || record.getMessage().contains(query.keyword());
    }

    private AdminLogAppliedFiltersVO toAppliedFilters(NormalizedQuery query) {
        AdminLogAppliedFiltersVO appliedFiltersVO = new AdminLogAppliedFiltersVO();
        appliedFiltersVO.setStartTime(query.startTime());
        appliedFiltersVO.setEndTime(query.endTime());
        appliedFiltersVO.setLevel(query.level());
        appliedFiltersVO.setKeyword(query.keyword());
        appliedFiltersVO.setLimit(query.limit());
        return appliedFiltersVO;
    }

    private record NormalizedQuery(
            LocalDateTime startTime,
            LocalDateTime endTime,
            String level,
            String keyword,
            int limit) {
    }
}
