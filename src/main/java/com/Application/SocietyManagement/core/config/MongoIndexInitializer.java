package com.Application.SocietyManagement.core.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.stereotype.Component;

/**
 * Ensures all required compound and unique MongoDB indexes exist at startup,
 * even when spring.data.mongodb.auto-index-creation is set to false in production.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MongoIndexInitializer implements ApplicationRunner {

    private final MongoTemplate mongoTemplate;
    private final MongoMappingContext mongoMappingContext;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Validating and ensuring MongoDB indexes for production domain entities...");
        try {
            MongoPersistentEntityIndexResolver resolver = new MongoPersistentEntityIndexResolver(mongoMappingContext);
            for (var entity : mongoMappingContext.getPersistentEntities()) {
                Class<?> type = entity.getType();
                if (type.isAnnotationPresent(Document.class)) {
                    IndexOperations indexOps = mongoTemplate.indexOps(type);
                    for (var index : resolver.resolveIndexFor(type)) {
                        try {
                            indexOps.ensureIndex(index);
                        } catch (Exception e) {
                            log.warn("Index check/creation warning for {}: {}", type.getSimpleName(), e.getMessage());
                        }
                    }
                }
            }
            log.info("MongoDB index validation successfully finished.");
        } catch (Exception e) {
            log.warn("MongoIndexInitializer encountered a non-fatal issue during index scan: {}", e.getMessage());
        }
    }
}
