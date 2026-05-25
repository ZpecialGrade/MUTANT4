package com.stylish.wardrobe.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class DmitriyAsyncConfig {

	@Bean(name = "activityExecutor")
	public Executor activityExecutor() {
		ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
		ex.setCorePoolSize(2);
		ex.setMaxPoolSize(4);
		ex.setQueueCapacity(500);
		ex.setThreadNamePrefix("wardrobe-activity-");
		ex.initialize();
		return ex;
	}
}
