package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tepinhui.tepinhui_backend.config.ResultHttpStatusAdvice;
import com.tepinhui.tepinhui_backend.dto.product.ProductCreateRequest;
import com.tepinhui.tepinhui_backend.exception.GlobalExceptionHandler;
import com.tepinhui.tepinhui_backend.mapper.MerchantMapper;
import com.tepinhui.tepinhui_backend.mapper.ProductMapper;
import com.tepinhui.tepinhui_backend.mapper.UserMapper;
import com.tepinhui.tepinhui_backend.security.JwtAuthenticationFilter;
import com.tepinhui.tepinhui_backend.security.JwtUtil;
import com.tepinhui.tepinhui_backend.service.ProductService;
import com.tepinhui.tepinhui_backend.vo.product.ProductDetailVO;
import com.tepinhui.tepinhui_backend.vo.product.ProductListVO;
import com.tepinhui.tepinhui_backend.vo.product.ProductPageVO;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@ContextConfiguration(classes = {
    ProductController.class,
    GlobalExceptionHandler.class,
    ResultHttpStatusAdvice.class,
    SecurityConfig.class,
    ProductControllerTest.NoOpJwtFilterConfig.class
})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private ProductMapper productMapper;

    @MockitoBean
    private MerchantMapper merchantMapper;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void pageProductsShouldReturnPublicPage() throws Exception {
        ProductListVO record = new ProductListVO();
        record.setId(11L);
        record.setName("西湖龙井");
        record.setStatus("on");
        record.setPrice(new BigDecimal("128.00"));

        ProductPageVO pageVO = new ProductPageVO();
        pageVO.setRecords(List.of(record));
        pageVO.setTotal(1L);
        pageVO.setPage(1L);
        pageVO.setSize(10L);

        when(productService.pageProducts(any())).thenReturn(pageVO);

        mockMvc.perform(get("/api/v1/products")
                        .param("page", "1")
                        .param("size", "10")
                        .param("keyword", "龙井"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value(11))
                .andExpect(jsonPath("$.data.records[0].status").value("on"))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(productService).pageProducts(any());
    }

    @Test
    void getProductDetailShouldReturnPublicDetail() throws Exception {
        ProductDetailVO detailVO = new ProductDetailVO();
        detailVO.setId(21L);
        detailVO.setMerchantId(5L);
        detailVO.setName("安吉白茶");
        detailVO.setStatus("on");
        detailVO.setPrice(new BigDecimal("199.00"));

        when(productService.getProductDetail(21L)).thenReturn(detailVO);

        mockMvc.perform(get("/api/v1/products/21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(21))
                .andExpect(jsonPath("$.data.name").value("安吉白茶"))
                .andExpect(jsonPath("$.data.status").value("on"));
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void createProductShouldRejectInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "specialtyId": null,
                                  "name": " ",
                                  "price": 0,
                                  "stock": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("特产ID不能为空")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("商品名不能为空")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("商品价格必须大于0")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("商品库存不能小于0")));

        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void updateProductShouldRejectInvalidRequest() throws Exception {
        mockMvc.perform(put("/api/v1/products/33")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "specialtyId": 9,
                                  "name": "",
                                  "price": 0.00,
                                  "stock": -2
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("商品名不能为空")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("商品价格必须大于0")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("商品库存不能小于0")));

        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProductShouldCallService() throws Exception {
        doNothing().when(productService).deleteProduct(45L);

        mockMvc.perform(delete("/api/v1/products/45"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("商品删除成功"));

        verify(productService).deleteProduct(45L);
    }

    @Test
    void createProductShouldRejectAnonymousUser() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequestJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("未登录"));

        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void createProductShouldRejectConsumerRole() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequestJson()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(productService);
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void createProductShouldAllowMerchantRole() throws Exception {
        when(productService.createProduct(any(ProductCreateRequest.class))).thenReturn(88L);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("商品创建成功"))
                .andExpect(jsonPath("$.data").value(88));

        verify(productService).createProduct(any(ProductCreateRequest.class));
    }

    private String validCreateRequestJson() {
        return """
                {
                  "specialtyId": 6,
                  "name": "武义宣莲",
                  "description": "测试商品",
                  "price": 66.80,
                  "stock": 12,
                  "images": "[\\"a.png\\"]"
                }
                """;
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
