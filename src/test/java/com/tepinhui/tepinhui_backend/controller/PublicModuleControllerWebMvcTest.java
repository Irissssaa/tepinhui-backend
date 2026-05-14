package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tepinhui.tepinhui_backend.config.ResultHttpStatusAdvice;
import com.tepinhui.tepinhui_backend.exception.GlobalExceptionHandler;
import com.tepinhui.tepinhui_backend.security.JwtAuthenticationFilter;
import com.tepinhui.tepinhui_backend.security.JwtUtil;
import com.tepinhui.tepinhui_backend.service.CategoryService;
import com.tepinhui.tepinhui_backend.service.MapService;
import com.tepinhui.tepinhui_backend.service.SpecialtyService;
import com.tepinhui.tepinhui_backend.service.StatsService;
import com.tepinhui.tepinhui_backend.vo.category.CategoryVO;
import com.tepinhui.tepinhui_backend.vo.map.SeasonRecommendVO;
import com.tepinhui.tepinhui_backend.vo.map.SpecialtyMapVO;
import com.tepinhui.tepinhui_backend.vo.specialty.SpecialtyDetailVO;
import com.tepinhui.tepinhui_backend.vo.specialty.SpecialtyListVO;
import com.tepinhui.tepinhui_backend.vo.stats.TraceStatsVO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({
    CategoryController.class,
    SpecialtyController.class,
    MapController.class,
    StatsController.class
})
@ContextConfiguration(classes = {
    CategoryController.class,
    SpecialtyController.class,
    MapController.class,
    StatsController.class,
    GlobalExceptionHandler.class,
    ResultHttpStatusAdvice.class,
    SecurityConfig.class,
    PublicModuleControllerWebMvcTest.NoOpJwtFilterConfig.class
})
class PublicModuleControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private SpecialtyService specialtyService;

    @MockitoBean
    private MapService mapService;

    @MockitoBean
    private StatsService statsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void anonymousShouldAccessCategoryList() throws Exception {
        CategoryVO categoryVO = new CategoryVO();
        categoryVO.setId(1L);
        categoryVO.setName("名茶");

        when(categoryService.listCategories()).thenReturn(List.of(categoryVO));

        mockMvc.perform(get("/api/v1/categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].name").value("名茶"));

        verify(categoryService).listCategories();
    }

    @Test
    void anonymousShouldAccessSpecialtyList() throws Exception {
        SpecialtyListVO listVO = new SpecialtyListVO();
        listVO.setId(3L);
        listVO.setName("西湖龙井");

        when(specialtyService.listSpecialties()).thenReturn(List.of(listVO));

        mockMvc.perform(get("/api/v1/specialties"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("success"))
            .andExpect(jsonPath("$.data[0].id").value(3))
            .andExpect(jsonPath("$.data[0].name").value("西湖龙井"));

        verify(specialtyService).listSpecialties();
    }

    @Test
    void anonymousShouldAccessSpecialtyDetail() throws Exception {
        SpecialtyDetailVO detailVO = new SpecialtyDetailVO();
        detailVO.setId(5L);
        detailVO.setName("安吉白茶");

        when(specialtyService.getSpecialtyDetail(5L)).thenReturn(detailVO);

        mockMvc.perform(get("/api/v1/specialties/5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.id").value(5))
            .andExpect(jsonPath("$.data.name").value("安吉白茶"));

        verify(specialtyService).getSpecialtyDetail(5L);
    }

    @Test
    void anonymousShouldAccessMapSpecialties() throws Exception {
        SpecialtyMapVO mapVO = new SpecialtyMapVO();
        mapVO.setSpecialtyName("金华火腿");

        when(mapService.listSpecialties()).thenReturn(List.of(mapVO));

        mockMvc.perform(get("/api/v1/map/specialties"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].specialtyName").value("金华火腿"));

        verify(mapService).listSpecialties();
    }

    @Test
    void anonymousShouldAccessSeasonRecommend() throws Exception {
        SeasonRecommendVO recommendVO = new SeasonRecommendVO();
        recommendVO.setName("杨梅");

        when(mapService.listSeasonRecommendations()).thenReturn(List.of(recommendVO));

        mockMvc.perform(get("/api/v1/map/season-recommend"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].name").value("杨梅"));

        verify(mapService).listSeasonRecommendations();
    }

    @Test
    void anonymousShouldAccessTraceStats() throws Exception {
        TraceStatsVO statsVO = new TraceStatsVO();
        statsVO.setTotalTraceCount(10L);
        statsVO.setPassedTraceCount(6L);
        statsVO.setPendingTraceCount(3L);
        statsVO.setRejectedTraceCount(1L);

        when(statsService.getTraceStats()).thenReturn(statsVO);

        mockMvc.perform(get("/api/v1/stats/trace"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalTraceCount").value(10))
            .andExpect(jsonPath("$.data.pendingTraceCount").value(3));

        verify(statsService).getTraceStats();
    }

    @TestConfiguration
    static class NoOpJwtFilterConfig {

        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtil jwtUtil,
                                                        StringRedisTemplate stringRedisTemplate,
                                                        ObjectMapper objectMapper) {
            return new JwtAuthenticationFilter(jwtUtil, stringRedisTemplate, objectMapper) {
                @Override
                protected void doFilterInternal(HttpServletRequest request,
                                                HttpServletResponse response,
                                                FilterChain filterChain) throws ServletException, IOException {
                    filterChain.doFilter(request, response);
                }
            };
        }
    }
}
