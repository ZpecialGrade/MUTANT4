package com.stylish.auth.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

	@Bean(name = "auditExecutor")
	public Executor auditExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(2);
		ex.setMaxPoolSize(4);
		ex.setQueueCapacity(200);
		ex.setThreadNamePrefix("auth-audit-");
		ex.initialize();
		return ex;
	}
}
