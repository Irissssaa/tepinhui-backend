package com.tepinhui.tepinhui_backend.controller;

import com.tepinhui.tepinhui_backend.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tepinhui.tepinhui_backend.config.TrailingSlashCompatibilityConfig;
import com.tepinhui.tepinhui_backend.config.ResultHttpStatusAdvice;
import com.tepinhui.tepinhui_backend.exception.GlobalExceptionHandler;
import com.tepinhui.tepinhui_backend.security.JwtAuthenticationFilter;
import com.tepinhui.tepinhui_backend.security.JwtUtil;
import com.tepinhui.tepinhui_backend.service.AddressService;
import com.tepinhui.tepinhui_backend.service.CartService;
import com.tepinhui.tepinhui_backend.service.OrderService;
import com.tepinhui.tepinhui_backend.service.ReviewService;
import com.tepinhui.tepinhui_backend.service.UserProfileService;
import com.tepinhui.tepinhui_backend.vo.address.AddressVO;
import com.tepinhui.tepinhui_backend.vo.cart.CartVO;
import com.tepinhui.tepinhui_backend.vo.order.OrderDetailVO;
import com.tepinhui.tepinhui_backend.vo.order.OrderPageVO;
import com.tepinhui.tepinhui_backend.vo.review.ReviewVO;
import com.tepinhui.tepinhui_backend.vo.user.UserProfileVO;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

@WebMvcTest({
    UserController.class,
    AddressController.class,
    CartController.class,
    OrderController.class
})
@ContextConfiguration(classes = {
    UserController.class,
    AddressController.class,
    CartController.class,
    OrderController.class,
    GlobalExceptionHandler.class,
    ResultHttpStatusAdvice.class,
    SecurityConfig.class,
    TrailingSlashCompatibilityConfig.class,
    UserModuleControllerWebMvcTest.NoOpJwtFilterConfig.class,
})
class UserModuleControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private AddressService addressService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void anonymousShouldBeRejectedFromUserProfile() throws Exception {
        mockMvc.perform(get("/api/v1/user/profile"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("未登录"));

        verifyNoInteractions(userProfileService);
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void consumerShouldAccessUserProfile() throws Exception {
        UserProfileVO profileVO = new UserProfileVO();
        profileVO.setId(7L);
        profileVO.setUsername("alice");
        profileVO.setRole("CONSUMER");

        when(userProfileService.getCurrentUserProfile()).thenReturn(profileVO);

        mockMvc.perform(get("/api/v1/user/profile"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("success"))
            .andExpect(jsonPath("$.data.id").value(7))
            .andExpect(jsonPath("$.data.role").value("CONSUMER"));

        verify(userProfileService).getCurrentUserProfile();
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void merchantShouldAccessAddressList() throws Exception {
        mockMvc.perform(get("/api/v1/addresses"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(addressService);
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void consumerShouldAccessAddressList() throws Exception {
        AddressVO addressVO = new AddressVO();
        addressVO.setId(13L);
        addressVO.setConsignee("张三");

        when(addressService.listCurrentUserAddresses()).thenReturn(List.of(addressVO));

        mockMvc.perform(get("/api/v1/addresses"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data[0].id").value(13))
            .andExpect(jsonPath("$.data[0].consignee").value("张三"));

        verify(addressService).listCurrentUserAddresses();
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void consumerShouldCreateAddress() throws Exception {
        AddressVO addressVO = new AddressVO();
        addressVO.setId(21L);
        addressVO.setConsignee("李四");

        when(addressService.createAddress(any())).thenReturn(addressVO);

        mockMvc.perform(post("/api/v1/addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "consignee": "李四",
                      "phone": "13800138000",
                      "province": "浙江省",
                      "city": "杭州市",
                      "county": "西湖区",
                      "detail": "文三路 100 号",
                      "isDefault": 1
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("收货地址创建成功"))
            .andExpect(jsonPath("$.data.id").value(21));

        verify(addressService).createAddress(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminShouldAccessCartList() throws Exception {
        CartVO cartVO = new CartVO();
        cartVO.setItems(List.of());
        cartVO.setTotalQuantity(0);
        cartVO.setTotalAmount(BigDecimal.ZERO);

        when(cartService.getCurrentUserCart()).thenReturn(cartVO);

        mockMvc.perform(get("/api/v1/cart"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalQuantity").value(0))
            .andExpect(jsonPath("$.data.totalAmount").value(0));

        verify(cartService).getCurrentUserCart();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminShouldAccessCartListWithTrailingSlash() throws Exception {
        CartVO cartVO = new CartVO();
        cartVO.setItems(List.of());
        cartVO.setTotalQuantity(0);
        cartVO.setTotalAmount(BigDecimal.ZERO);

        when(cartService.getCurrentUserCart()).thenReturn(cartVO);

        mockMvc.perform(get("/api/v1/cart/"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.totalQuantity").value(0));

        verify(cartService).getCurrentUserCart();
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void merchantShouldBeRejectedFromCartWrite() throws Exception {
        mockMvc.perform(post("/api/v1/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "productId": 1,
                      "quantity": 2
                    }
                    """))
            .andExpect(status().isForbidden());

        verifyNoInteractions(cartService);
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void consumerShouldAddCartItem() throws Exception {
        doNothing().when(cartService).addCartItem(any());

        mockMvc.perform(post("/api/v1/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "productId": 1,
                      "quantity": 2
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("加入购物车成功"))
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(cartService).addCartItem(any());
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void consumerShouldCreateOrder() throws Exception {
        OrderDetailVO detailVO = new OrderDetailVO();
        detailVO.setId(31L);
        detailVO.setOrderNo("ORD-001");
        detailVO.setStatus("pending");

        when(orderService.createOrder(any())).thenReturn(detailVO);

        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "addressId": 9,
                      "remark": "请尽快发货",
                      "items": [
                        {
                          "productId": 101,
                          "quantity": 2
                        }
                      ]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("订单创建成功"))
            .andExpect(jsonPath("$.data.id").value(31))
            .andExpect(jsonPath("$.data.status").value("pending"));

        verify(orderService).createOrder(any());
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void merchantShouldShipOrder() throws Exception {
        doNothing().when(orderService).shipOrder(any(), any());

        mockMvc.perform(put("/api/v1/orders/31/ship")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "logisticsNo": "SF1234567890"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("订单发货成功"))
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(orderService).shipOrder(any(), any());
    }

    @Test
    @WithMockUser(roles = "MERCHANT")
    void merchantShouldBeRejectedFromOrderList() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(orderService);
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void consumerShouldAccessOrderList() throws Exception {
        OrderPageVO pageVO = new OrderPageVO();
        pageVO.setRecords(List.of());
        pageVO.setTotal(0L);
        pageVO.setPage(1);
        pageVO.setSize(10);

        when(orderService.getCurrentUserOrders(any())).thenReturn(pageVO);

        mockMvc.perform(get("/api/v1/orders")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.total").value(0));

        verify(orderService).getCurrentUserOrders(any());
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void consumerShouldAccessOrderListWithTrailingSlash() throws Exception {
        OrderPageVO pageVO = new OrderPageVO();
        pageVO.setRecords(List.of());
        pageVO.setTotal(0L);
        pageVO.setPage(1);
        pageVO.setSize(10);

        when(orderService.getCurrentUserOrders(any())).thenReturn(pageVO);

        mockMvc.perform(get("/api/v1/orders/")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.total").value(0));

        verify(orderService).getCurrentUserOrders(any());
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void consumerShouldCreateOrderReview() throws Exception {
        ReviewVO reviewVO = new ReviewVO();
        reviewVO.setId(41L);
        reviewVO.setRating(5);

        when(reviewService.createOrderReview(any(), any())).thenReturn(reviewVO);

        mockMvc.perform(post("/api/v1/orders/31/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "rating": 5,
                      "content": "很好"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("订单评价提交成功"))
            .andExpect(jsonPath("$.data.id").value(41))
            .andExpect(jsonPath("$.data.rating").value(5));

        verify(reviewService).createOrderReview(any(), any());
    }

    @Test
    @WithMockUser(roles = "CONSUMER")
    void consumerShouldDeleteAddress() throws Exception {
        doNothing().when(addressService).deleteAddress(21L);

        mockMvc.perform(delete("/api/v1/addresses/21"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("收货地址删除成功"))
            .andExpect(jsonPath("$.data").doesNotExist());

        verify(addressService).deleteAddress(21L);
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
