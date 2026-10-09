package com.Application.SocietyManagement.core.tenant;

import com.Application.SocietyManagement.society.entity.Society;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enterprise Level 2 Multi-Tenant MongoTemplate.
 * Automatically intercepts every read, count, exists, update, remove, aggregation,
 * and persistence operation executed via Spring Data Repositories and MongoTemplate,
 * injecting the active tenant's societyId and preventing cross-tenant data leaks.
 */
@Slf4j
public class MultiTenantMongoTemplate extends MongoTemplate {

    private static final String TENANT_KEY = "societyId";
    private static final Map<Class<?>, Field> TENANT_FIELD_CACHE = new ConcurrentHashMap<>();

    public MultiTenantMongoTemplate(MongoDatabaseFactory mongoDbFactory, MongoConverter mongoConverter) {
        super(mongoDbFactory, mongoConverter);
    }

    // ── READ INTERCEPTION ──

    @Override
    public <T> List<T> find(Query query, Class<T> entityClass, String collectionName) {
        applyTenantFilter(query, entityClass, collectionName);
        return super.find(query, entityClass, collectionName);
    }

    @Override
    public <T> T findById(Object id, Class<T> entityClass, String collectionName) {
        Query query = new Query(Criteria.where("_id").is(id));
        applyTenantFilter(query, entityClass, collectionName);
        return findOne(query, entityClass, collectionName);
    }

    @Override
    public <T> T findOne(Query query, Class<T> entityClass, String collectionName) {
        applyTenantFilter(query, entityClass, collectionName);
        return super.findOne(query, entityClass, collectionName);
    }

    @Override
    public <T> T findAndModify(Query query, UpdateDefinition update, FindAndModifyOptions options, Class<T> entityClass, String collectionName) {
        applyTenantFilter(query, entityClass, collectionName);
        return super.findAndModify(query, update, options, entityClass, collectionName);
    }

    @Override
    public <T> T findAndRemove(Query query, Class<T> entityClass, String collectionName) {
        applyTenantFilter(query, entityClass, collectionName);
        return super.findAndRemove(query, entityClass, collectionName);
    }

    @Override
    public long count(Query query, Class<?> entityClass, String collectionName) {
        applyTenantFilter(query, entityClass, collectionName);
        return super.count(query, entityClass, collectionName);
    }

    @Override
    public boolean exists(Query query, Class<?> entityClass, String collectionName) {
        applyTenantFilter(query, entityClass, collectionName);
        return super.exists(query, entityClass, collectionName);
    }

    // ── WRITE & MUTATION INTERCEPTION ──

    @Override
    public DeleteResult remove(Query query, Class<?> entityClass, String collectionName) {
        applyTenantFilter(query, entityClass, collectionName);
        return super.remove(query, entityClass, collectionName);
    }

    @Override
    public UpdateResult updateFirst(Query query, UpdateDefinition update, Class<?> entityClass, String collectionName) {
        applyTenantFilter(query, entityClass, collectionName);
        return super.updateFirst(query, update, entityClass, collectionName);
    }

    @Override
    public UpdateResult updateMulti(Query query, UpdateDefinition update, Class<?> entityClass, String collectionName) {
        applyTenantFilter(query, entityClass, collectionName);
        return super.updateMulti(query, update, entityClass, collectionName);
    }

    @Override
    public <T> T save(T objectToSave, String collectionName) {
        enforceTenantOnEntity(objectToSave, collectionName);
        return super.save(objectToSave, collectionName);
    }

    @Override
    public <T> T insert(T objectToSave, String collectionName) {
        enforceTenantOnEntity(objectToSave, collectionName);
        return super.insert(objectToSave, collectionName);
    }

    @Override
    public <T> Collection<T> insert(Collection<? extends T> batchToSave, String collectionName) {
        if (batchToSave != null) {
            for (T item : batchToSave) {
                enforceTenantOnEntity(item, collectionName);
            }
        }
        return super.insert(batchToSave, collectionName);
    }

    // ── AGGREGATION INTERCEPTION ──

    @Override
    public <O> AggregationResults<O> aggregate(Aggregation aggregation, String collectionName, Class<O> outputType) {
        if (isTenantScoped(null, collectionName)) {
            String tenantId = TenantContext.getSocietyId();
            if (tenantId != null && !tenantId.isBlank()) {
                aggregation = injectTenantToAggregation(aggregation, tenantId);
            }
        }
        return super.aggregate(aggregation, collectionName, outputType);
    }

    @Override
    public <O> AggregationResults<O> aggregate(TypedAggregation<?> aggregation, String collectionName, Class<O> outputType) {
        if (isTenantScoped(aggregation.getInputType(), collectionName)) {
            String tenantId = TenantContext.getSocietyId();
            if (tenantId != null && !tenantId.isBlank()) {
                aggregation = injectTenantToTypedAggregation(aggregation, tenantId);
            }
        }
        return super.aggregate(aggregation, collectionName, outputType);
    }

    // ── TENANT ENGINE INTERNALS ──

    void applyTenantFilter(Query query, Class<?> entityClass, String collectionName) {
        if (query == null || !isTenantScoped(entityClass, collectionName)) {
            return;
        }

        String tenantId = TenantContext.getSocietyId();
        if (tenantId == null || tenantId.isBlank()) {
            return; // System or public mode
        }

        Document queryObject = query.getQueryObject();
        if (queryObject.containsKey(TENANT_KEY)) {
            Object existingValue = queryObject.get(TENANT_KEY);
            if (existingValue instanceof String existingStr) {
                if (!existingStr.equals(tenantId)) {
                    throw new AccessDeniedException(
                            "Cross-tenant query violation: Query target " + existingStr
                                    + " does not match authenticated tenant " + tenantId);
                }
            }
            return;
        }

        // Automatically append tenant isolation criteria
        query.addCriteria(Criteria.where(TENANT_KEY).is(tenantId));
    }

    void enforceTenantOnEntity(Object entity, String collectionName) {
        if (entity == null || !isTenantScoped(entity.getClass(), collectionName)) {
            return;
        }

        String tenantId = TenantContext.getSocietyId();
        if (tenantId == null || tenantId.isBlank()) {
            return;
        }

        Field field = findTenantField(entity.getClass());
        if (field == null) {
            return;
        }

        try {
            field.setAccessible(true);
            Object currentValue = field.get(entity);
            if (currentValue == null) {
                field.set(entity, tenantId);
            } else if (!Objects.equals(currentValue.toString(), tenantId)) {
                throw new AccessDeniedException(
                        "Cross-tenant mutation violation: Entity belongs to society " + currentValue
                                + " but attempted save under tenant context " + tenantId);
            }
        } catch (AccessDeniedException ade) {
            throw ade;
        } catch (Exception e) {
            log.warn("Could not enforce tenant on entity {}: {}", entity.getClass().getSimpleName(), e.getMessage());
        }
    }

    Aggregation injectTenantToAggregation(Aggregation aggregation, String tenantId) {
        validateAggregationPipeline(aggregation, tenantId);
        List<AggregationOperation> operations = new ArrayList<>();
        operations.add(Aggregation.match(Criteria.where(TENANT_KEY).is(tenantId)));
        operations.addAll(aggregation.getPipeline().getOperations());
        return Aggregation.newAggregation(operations);
    }

    @SuppressWarnings("unchecked")
    <T> TypedAggregation<T> injectTenantToTypedAggregation(TypedAggregation<T> aggregation, String tenantId) {
        validateAggregationPipeline(aggregation, tenantId);
        List<AggregationOperation> operations = new ArrayList<>();
        operations.add(Aggregation.match(Criteria.where(TENANT_KEY).is(tenantId)));
        operations.addAll(aggregation.getPipeline().getOperations());
        return (TypedAggregation<T>) Aggregation.newAggregation(aggregation.getInputType(), operations);
    }

    void validateAggregationPipeline(Aggregation aggregation, String tenantId) {
        for (AggregationOperation op : aggregation.getPipeline().getOperations()) {
            Document doc = op.toDocument(Aggregation.DEFAULT_CONTEXT);
            if (doc.containsKey("$match")) {
                Document matchDoc = doc.get("$match", Document.class);
                if (matchDoc != null && matchDoc.containsKey(TENANT_KEY)) {
                    Object val = matchDoc.get(TENANT_KEY);
                    if (val instanceof String strVal && !strVal.equals(tenantId)) {
                        throw new AccessDeniedException(
                                "Cross-tenant aggregation violation: Pipeline $match targets society " + strVal
                                        + " but authenticated tenant is " + tenantId);
                    }
                }
            }
            if (doc.containsKey("$lookup")) {
                Document lookupDoc = doc.get("$lookup", Document.class);
                if (lookupDoc != null && lookupDoc.containsKey("pipeline")) {
                    List<?> subPipeline = lookupDoc.get("pipeline", List.class);
                    if (subPipeline != null) {
                        for (Object stage : subPipeline) {
                            if (stage instanceof Document stageDoc && stageDoc.containsKey("$match")) {
                                Document subMatch = stageDoc.get("$match", Document.class);
                                if (subMatch != null && subMatch.containsKey(TENANT_KEY)) {
                                    Object val = subMatch.get(TENANT_KEY);
                                    if (val instanceof String strVal && !strVal.equals(tenantId)) {
                                        throw new AccessDeniedException(
                                                "Cross-tenant lookup violation: Sub-pipeline $match targets society " + strVal
                                                        + " but authenticated tenant is " + tenantId);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isTenantScoped(Class<?> entityClass, String collectionName) {
        if (entityClass != null && Society.class.isAssignableFrom(entityClass)) {
            return false;
        }
        return !"societies".equalsIgnoreCase(collectionName);
    }

    private Field findTenantField(Class<?> clazz) {
        return TENANT_FIELD_CACHE.computeIfAbsent(clazz, c -> {
            Class<?> current = c;
            while (current != null && current != Object.class) {
                try {
                    return current.getDeclaredField(TENANT_KEY);
                } catch (NoSuchFieldException ignored) {
                    current = current.getSuperclass();
                }
            }
            return null;
        });
    }
}
