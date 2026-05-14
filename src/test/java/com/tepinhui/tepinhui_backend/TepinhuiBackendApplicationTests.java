package com.tepinhui.tepinhui_backend;

import com.tepinhui.tepinhui_backend.mapper.AddressMapper;
import com.tepinhui.tepinhui_backend.mapper.CartItemMapper;
import com.tepinhui.tepinhui_backend.mapper.CategoryMapper;
import com.tepinhui.tepinhui_backend.mapper.CultureContentMapper;
import com.tepinhui.tepinhui_backend.mapper.MerchantMapper;
import com.tepinhui.tepinhui_backend.mapper.OrderItemMapper;
import com.tepinhui.tepinhui_backend.mapper.OrdersMapper;
import com.tepinhui.tepinhui_backend.mapper.OriginMapper;
import com.tepinhui.tepinhui_backend.mapper.ProductMapper;
import com.tepinhui.tepinhui_backend.mapper.ReviewMapper;
import com.tepinhui.tepinhui_backend.mapper.SpecialtyMapper;
import com.tepinhui.tepinhui_backend.mapper.TraceRecordMapper;
import com.tepinhui.tepinhui_backend.mapper.UserMapper;
import com.tepinhui.tepinhui_backend.service.RegisterVerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
	"spring.autoconfigure.exclude="
		+ "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
		+ "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
		+ "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
		+ "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
		+ "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration,"
		+ "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration",
	"spring.sql.init.mode=never"
})
class TepinhuiBackendApplicationTests {

	@MockitoBean
	private UserMapper userMapper;

	@MockitoBean
	private MerchantMapper merchantMapper;

	@MockitoBean
	private ProductMapper productMapper;

	@MockitoBean
	private TraceRecordMapper traceRecordMapper;

	@MockitoBean
	private OriginMapper originMapper;

	@MockitoBean
	private SpecialtyMapper specialtyMapper;

	@MockitoBean
	private OrdersMapper ordersMapper;

	@MockitoBean
	private OrderItemMapper orderItemMapper;

	@MockitoBean
	private AddressMapper addressMapper;

	@MockitoBean
	private CartItemMapper cartItemMapper;

	@MockitoBean
	private ReviewMapper reviewMapper;

	@MockitoBean
	private CategoryMapper categoryMapper;

	@MockitoBean
	private CultureContentMapper cultureContentMapper;

	@MockitoBean
	private RedisTemplate<String, Object> redisTemplate;

	@MockitoBean
	private StringRedisTemplate stringRedisTemplate;

	@MockitoBean
	private RegisterVerificationService registerVerificationService;

	@MockitoBean
	private JavaMailSender javaMailSender;

	@MockitoBean
	private RedisConnectionFactory redisConnectionFactory;

	@Test
	void contextLoads() {
	}

}
