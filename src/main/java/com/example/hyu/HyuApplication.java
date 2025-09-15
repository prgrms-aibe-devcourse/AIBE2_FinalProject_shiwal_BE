package com.example.hyu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching; // ✅ 추가
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;


@SpringBootApplication
@EnableCaching //캐시 기능 활성화
@EnableScheduling
@EnableMethodSecurity(prePostEnabled = true) //메서드 보안 활성화
public class HyuApplication {

	public static void main(String[] args) {
		SpringApplication.run(HyuApplication.class, args);
	}
	
}
