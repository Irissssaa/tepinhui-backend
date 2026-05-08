package com.tepinhui.tepinhui_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@MapperScan("com.tepinhui.tepinhui_backend.mapper")
@EnableCaching
public class TepinhuiBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(TepinhuiBackendApplication.class, args);
	}

}
