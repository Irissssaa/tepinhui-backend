package com.tepinhui.tepinhui_backend.service;

import com.tepinhui.tepinhui_backend.dto.admin.AdminLogQueryRequest;
import com.tepinhui.tepinhui_backend.exception.BusinessException;
import com.tepinhui.tepinhui_backend.service.impl.AdminLogServiceImpl;
import com.tepinhui.tepinhui_backend.vo.admin.AdminLogPageVO;
import com.tepinhui.tepinhui_backend.vo.admin.AdminLogRecordVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminLogServiceImplTest {

    @TempDir
    Path tempDir;

    @Test
    void getLogs_filtersByLevel() throws IOException {
        Path logFile = writeLogFile("level-filter.log", """
            2026-05-15 10:00:00.000 | INFO  | [main] | c.t.TestService | startup complete
            2026-05-15 10:01:00.000 | ERROR | [main] | c.t.TestService | first failure
            java.lang.IllegalStateException: boom
                at c.t.TestService.run(TestService.java:10)
            2026-05-15 10:02:00.000 | WARN  | [main] | c.t.TestService | low stock
            2026-05-15 10:03:00.000 | ERROR | [main] | c.t.TestService | second failure
            """);
        AdminLogServiceImpl service = new AdminLogServiceImpl(logFile.toString(), 10, 20);

        AdminLogQueryRequest request = new AdminLogQueryRequest();
        request.setLevel("ERROR");

        AdminLogPageVO page = service.getLogs(request);

        assertEquals(2, page.getReturnedCount());
        assertEquals(List.of("second failure", "first failure"),
            page.getRecords().stream().map(record -> record.getMessage()).toList());
        assertTrue(page.getRecords().stream().allMatch(record -> "ERROR".equals(record.getLevel())));
    }

    @Test
    void getLogs_filtersByTimeRange() throws IOException {
        Path logFile = writeLogFile("time-filter.log", """
            2026-05-15 10:00:00.000 | INFO  | [main] | c.t.TestService | before window
            2026-05-15 10:01:00.000 | INFO  | [main] | c.t.TestService | window start
            2026-05-15 10:02:00.000 | WARN  | [main] | c.t.TestService | window end
            2026-05-15 10:03:00.000 | ERROR | [main] | c.t.TestService | after window
            """);
        AdminLogServiceImpl service = new AdminLogServiceImpl(logFile.toString(), 10, 20);

        AdminLogQueryRequest request = new AdminLogQueryRequest();
        request.setStartTime(LocalDateTime.of(2026, 5, 15, 10, 1, 0));
        request.setEndTime(LocalDateTime.of(2026, 5, 15, 10, 2, 0));

        AdminLogPageVO page = service.getLogs(request);

        assertEquals(2, page.getReturnedCount());
        assertEquals(List.of("window end", "window start"),
            page.getRecords().stream().map(record -> record.getMessage()).toList());
    }

    @Test
    void getLogs_usesDefaultLimitWhenRequestLimitIsMissing() throws IOException {
        Path logFile = writeLogFile("default-limit.log", """
            2026-05-15 10:00:00.000 | INFO  | [main] | c.t.TestService | first
            2026-05-15 10:01:00.000 | INFO  | [main] | c.t.TestService | second
            2026-05-15 10:02:00.000 | INFO  | [main] | c.t.TestService | third
            """);
        AdminLogServiceImpl service = new AdminLogServiceImpl(logFile.toString(), 2, 5);

        AdminLogPageVO page = service.getLogs(new AdminLogQueryRequest());

        assertEquals(2, page.getAppliedFilters().getLimit());
        assertEquals(2, page.getReturnedCount());
        assertEquals(List.of("third", "second"),
            page.getRecords().stream().map(record -> record.getMessage()).toList());
    }

    @Test
    void getLogs_rejectsLimitAboveMax() throws IOException {
        Path logFile = writeLogFile("limit-max.log", """
            2026-05-15 10:00:00.000 | INFO  | [main] | c.t.TestService | only line
            """);
        AdminLogServiceImpl service = new AdminLogServiceImpl(logFile.toString(), 2, 5);

        AdminLogQueryRequest request = new AdminLogQueryRequest();
        request.setLimit(6);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getLogs(request));

        assertEquals(400, ex.getCode());
        assertEquals("limit 必须在 1 到 5 之间", ex.getMessage());
    }

    @Test
    void getLogs_throwsWhenFileDoesNotExist() {
        Path missingFile = tempDir.resolve("missing.log");
        AdminLogServiceImpl service = new AdminLogServiceImpl(missingFile.toString(), 10, 20);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getLogs(new AdminLogQueryRequest()));

        assertEquals(500, ex.getCode());
        assertEquals("日志文件不存在", ex.getMessage());
    }

    @Test
    void getLogs_throwsWhenFileIsNotReadable() throws IOException {
        Path logFile = writeLogFile("unreadable.log", """
            2026-05-15 10:00:00.000 | INFO  | [main] | c.t.TestService | hidden
            """).normalize();
        AdminLogServiceImpl service = new AdminLogServiceImpl(logFile.toString(), 10, 20);

        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {
            files.when(() -> Files.exists(logFile)).thenReturn(true);
            files.when(() -> Files.isRegularFile(logFile)).thenReturn(true);
            files.when(() -> Files.isReadable(logFile)).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getLogs(new AdminLogQueryRequest()));

            assertEquals(500, ex.getCode());
            assertEquals("日志文件无读取权限", ex.getMessage());
        }
    }

    @Test
    void getLogs_capturesStackTraceLines() throws IOException {
        Path logFile = writeLogFile("stack-trace.log", """
            2026-05-15 10:00:00.000 | ERROR | [main] | c.t.TestService | request failed
            java.lang.IllegalStateException: boom
                at c.t.TestService.run(TestService.java:10)
            Caused by: java.lang.IllegalArgumentException: invalid
                at c.t.TestService.parse(TestService.java:24)
            2026-05-15 10:01:00.000 | INFO  | [main] | c.t.TestService | recovered
            """);
        AdminLogServiceImpl service = new AdminLogServiceImpl(logFile.toString(), 10, 20);

        AdminLogPageVO page = service.getLogs(new AdminLogQueryRequest());

        assertEquals(2, page.getReturnedCount());
        assertEquals(List.of("recovered", "request failed"),
            page.getRecords().stream().map(AdminLogRecordVO::getMessage).toList());

        // "recovered" has no stack trace
        assertNull(page.getRecords().get(0).getStackTrace());

        // "request failed" has full stack trace
        AdminLogRecordVO failed = page.getRecords().get(1);
        assertNotNull(failed.getStackTrace());
        assertTrue(failed.getStackTrace().contains("java.lang.IllegalStateException: boom"));
        assertTrue(failed.getStackTrace().contains("at c.t.TestService.run(TestService.java:10)"));
        assertTrue(failed.getStackTrace().contains("Caused by: java.lang.IllegalArgumentException: invalid"));
        assertTrue(failed.getStackTrace().contains("at c.t.TestService.parse(TestService.java:24)"));
    }

    @Test
    void getLogs_keywordMatchesInsideStackTrace() throws IOException {
        Path logFile = writeLogFile("keyword-stack.log", """
            2026-05-15 10:00:00.000 | ERROR | [main] | c.t.TestService | request failed
            java.lang.NullPointerException: null
                at com.tepinhui.service.ProductServiceImpl.getProduct(ProductServiceImpl.java:42)
            2026-05-15 10:01:00.000 | INFO  | [main] | c.t.TestService | all good
            """);
        AdminLogServiceImpl service = new AdminLogServiceImpl(logFile.toString(), 10, 20);

        AdminLogQueryRequest request = new AdminLogQueryRequest();
        request.setKeyword("NullPointerException");

        AdminLogPageVO page = service.getLogs(request);

        assertEquals(1, page.getReturnedCount());
        assertEquals("request failed", page.getRecords().get(0).getMessage());
    }

    @Test
    void getLogs_multipleErrorsWithSeparateStackTraces() throws IOException {
        Path logFile = writeLogFile("multi-stack.log", """
            2026-05-15 10:00:00.000 | ERROR | [main] | c.t.TestService | first error
            java.lang.IllegalStateException: first
                at c.t.TestService.method1(TestService.java:10)
            2026-05-15 10:01:00.000 | ERROR | [main] | c.t.TestService | second error
            java.lang.NullPointerException: second
                at c.t.TestService.method2(TestService.java:20)
            """);
        AdminLogServiceImpl service = new AdminLogServiceImpl(logFile.toString(), 10, 20);

        AdminLogPageVO page = service.getLogs(new AdminLogQueryRequest());

        assertEquals(2, page.getReturnedCount());

        AdminLogRecordVO second = page.getRecords().get(0);
        assertEquals("second error", second.getMessage());
        assertNotNull(second.getStackTrace());
        assertTrue(second.getStackTrace().contains("NullPointerException: second"));
        assertTrue(second.getStackTrace().contains("at c.t.TestService.method2"));

        AdminLogRecordVO first = page.getRecords().get(1);
        assertEquals("first error", first.getMessage());
        assertNotNull(first.getStackTrace());
        assertTrue(first.getStackTrace().contains("IllegalStateException: first"));
        assertTrue(first.getStackTrace().contains("at c.t.TestService.method1"));
    }

    @Test
    void getLogs_noOrphanStackAtFileStart() throws IOException {
        Path logFile = writeLogFile("no-orphan.log", """
            2026-05-15 10:00:00.000 | ERROR | [main] | c.t.TestService | failed
            java.lang.RuntimeException: boom
                at c.t.TestService.run(TestService.java:5)
            """);
        AdminLogServiceImpl service = new AdminLogServiceImpl(logFile.toString(), 10, 20);

        AdminLogPageVO page = service.getLogs(new AdminLogQueryRequest());

        assertEquals(1, page.getReturnedCount());
        AdminLogRecordVO record = page.getRecords().get(0);
        assertEquals("failed", record.getMessage());
        assertNotNull(record.getStackTrace());
        assertTrue(record.getStackTrace().contains("java.lang.RuntimeException: boom"));
    }

    private Path writeLogFile(String fileName, String content) throws IOException {
        Path logFile = tempDir.resolve(fileName);
        Files.writeString(logFile, content, StandardCharsets.UTF_8);
        return logFile;
    }
}
