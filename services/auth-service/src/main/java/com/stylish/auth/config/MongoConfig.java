package com.stylish.auth.config;

import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

	@Value("${MONGO_URI:mongodb://localhost:27017/authdb}")
	private String uri;

	@Override
	protected String getDatabaseName() {
		String db = new ConnectionString(uri).getDatabase();
		return db != null ? db : "authdb";
	}

	@Override
	public MongoClient mongoClient() {
		return MongoClients.create(uri);
	}

	@Override
	protected Collection<String> getMappingBasePackages() {
		return Collections.singleton("com.stylish.auth");
	}
}
