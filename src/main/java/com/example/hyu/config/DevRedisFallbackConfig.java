package com.example.hyu.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 개발 환경에서 Redis가 실제로 떠 있지 않아도
 * StringRedisTemplate 의존성 때문에 부팅이 막히지 않도록 해주는 fallback 설정.
 *
 * - 실제 연결은 템플릿 사용 시점에 일어남(기본).
 * - dev에서 토큰 블랙리스트 등의 Redis 기능을 안 쓰면 부팅만 통과해도 충분.
 * - 운영/실사용에선 반드시 실제 Redis를 붙이세요.
 */
@Configuration
@ConditionalOnMissingBean(StringRedisTemplate.class)
public class DevRedisFallbackConfig {

    /**
     * 기본 로컬 Redis 포트(6379)로 연결 팩토리를 만듭니다.
     * Redis가 안 떠 있어도, 보통 '사용 시점'에 연결을 시도하므로 부팅은 통과합니다.
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // 필요 시 host/port를 yml에서 주입받도록 변경 가능
        return new LettuceConnectionFactory("127.0.0.1", 6379);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}