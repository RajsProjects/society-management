package com.Application.SocietyManagement.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableMongoAuditing
public class MongoConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of());
    }

    @Bean
    public MongoClientSettingsBuilderCustomizer mongoConnectionPoolCustomizer() {
        return builder -> builder.applyToConnectionPoolSettings(pool ->
                pool.maxSize(50)
                    .minSize(5)
                    .maxWaitTime(2000, TimeUnit.MILLISECONDS)
                    .maxConnectionIdleTime(30000, TimeUnit.MILLISECONDS)
        );
    }

    @Bean
    @ConditionalOnProperty(name = "spring.data.mongodb.transactions.enabled", havingValue = "true", matchIfMissing = false)
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    @Bean
    @org.springframework.context.annotation.Primary
    public org.springframework.data.mongodb.core.MongoTemplate mongoTemplate(
            MongoDatabaseFactory mongoDbFactory,
            org.springframework.data.mongodb.core.convert.MongoConverter converter) {
        return new com.Application.SocietyManagement.core.tenant.MultiTenantMongoTemplate(mongoDbFactory, converter);
    }
}
