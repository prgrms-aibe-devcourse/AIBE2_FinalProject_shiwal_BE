package com.example.hyu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching; // ✅ 추가
import com.example.hyu.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@SpringBootApplication
@EnableCaching //캐시 기능 활성화
@EnableConfigurationProperties(JwtProperties.class)

public class HyuApplication {

	public static void main(String[] args) {
		SpringApplication.run(HyuApplication.class, args);
	}
	
}
