package com.agentic.taskmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@SpringBootApplication
@EnableCaching
@EnableFeignClients(basePackages = "com.agentic.taskmanager.Feign")
@EnableAutoConfiguration(exclude = {
		RedisAutoConfiguration.class,
		RedisReactiveAutoConfiguration.class
})
public class AgenticAiTaskManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgenticAiTaskManagerApplication.class, args);
	}

	// UNCOMMENT THIS BEAN
	@Bean
	public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<String, String> template = new RedisTemplate<>();
		template.setConnectionFactory(connectionFactory);
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new StringRedisSerializer());
		return template;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.csrf().disable()
				.authorizeHttpRequests()
				.anyRequest().permitAll(); // Allow all requests

		return http.build();
	}

	@Bean
	public LettuceConnectionFactory redisConnectionFactory() {
		// HARDCODING 'redis' DIRECTLY FOR DEBUGGING PURPOSES
		System.out.println("DEBUG: Explicitly creating LettuceConnectionFactory with hardcoded host: redis and port: 6379");
		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("redis", 6379);

		// If you use a password, you would hardcode it here for this debug step,
		// but you said no password, so leave this commented.
		// config.setPassword("your_secure_redis_password");

		return new LettuceConnectionFactory(config);
	}
}