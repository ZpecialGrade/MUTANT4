package com.stylish.auth.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DmitriyFlywayConfig {

	@Bean(initMethod = "migrate")
	public Flyway flyway(DataSource dataSource) {
		return Flyway.configure()
				.dataSource(dataSource)
				.schemas("auth")
				.defaultSchema("auth")
				.createSchemas(true)
				.locations("classpath:db/migration")
				.baselineOnMigrate(true)
				.load();
	}

	@Bean
	public static BeanFactoryPostProcessor entityManagerFactoryDependsOnFlyway() {
		return new BeanFactoryPostProcessor() {
			@Override
			public void postProcessBeanFactory(ConfigurableListableBeanFactory bf) throws BeansException {
				for (String name : bf.getBeanNamesForType(jakarta.persistence.EntityManagerFactory.class, true, false)) {
					BeanDefinition bd = bf.getBeanDefinition(name);
					String[] deps = bd.getDependsOn();
					if (deps == null) {
						bd.setDependsOn("flyway");
					} else {
						String[] merged = new String[deps.length + 1];
						System.arraycopy(deps, 0, merged, 0, deps.length);
						merged[deps.length] = "flyway";
						bd.setDependsOn(merged);
					}
				}
			}
		};
	}
}
