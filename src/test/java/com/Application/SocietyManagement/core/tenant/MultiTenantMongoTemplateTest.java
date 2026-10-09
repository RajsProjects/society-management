package com.Application.SocietyManagement.core.tenant;

import com.Application.SocietyManagement.complaint.entity.Complaint;
import com.Application.SocietyManagement.core.config.AsyncConfig;
import com.Application.SocietyManagement.society.entity.Society;
import com.Application.SocietyManagement.users.entity.User;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("Multi-Tenant Isolation & Adversarial Test Suite")
class MultiTenantMongoTemplateTest {

    @Mock
    private MongoDatabaseFactory mongoDatabaseFactory;

    private MultiTenantMongoTemplate template;

    @BeforeEach
    void setUp() {
        lenient().when(mongoDatabaseFactory.getExceptionTranslator())
                .thenReturn(new org.springframework.data.mongodb.core.MongoExceptionTranslator());
        org.springframework.data.mongodb.core.convert.DbRefResolver dbRefResolver =
                new org.springframework.data.mongodb.core.convert.DefaultDbRefResolver(mongoDatabaseFactory);
        org.springframework.data.mongodb.core.convert.MappingMongoConverter converter =
                new org.springframework.data.mongodb.core.convert.MappingMongoConverter(
                        dbRefResolver, new org.springframework.data.mongodb.core.mapping.MongoMappingContext());
        converter.afterPropertiesSet();
        template = new MultiTenantMongoTemplate(mongoDatabaseFactory, converter);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    // ── 1. READ / QUERY SCENARIOS ──

    @Test
    @DisplayName("findById() across tenants: Injects societyId to prevent cross-tenant lookup by ID")
    void findById_acrossTenants_injectsTenantCriteria() {
        TenantContext.setSocietyId("soc-alpha");

        Query query = new Query(Criteria.where("_id").is("complaint-999"));
        template.applyTenantFilter(query, Complaint.class, "complaints");

        Document queryObject = query.getQueryObject();
        assertThat(queryObject.get("_id")).isEqualTo("complaint-999");
        assertThat(queryObject.get("societyId")).isEqualTo("soc-alpha");
    }

    @Test
    @DisplayName("findAll() across tenants: Unscoped query receives active societyId filter")
    void findAll_acrossTenants_injectsTenantCriteria() {
        TenantContext.setSocietyId("soc-alpha");

        Query query = new Query();
        template.applyTenantFilter(query, Complaint.class, "complaints");

        Document queryObject = query.getQueryObject();
        assertThat(queryObject.get("societyId")).isEqualTo("soc-alpha");
    }

    @Test
    @DisplayName("Query with foreign societyId: Throws AccessDeniedException")
    void query_withForeignSocietyId_throwsAccessDenied() {
        TenantContext.setSocietyId("soc-alpha");

        Query query = new Query(Criteria.where("societyId").is("soc-hacker"));

        assertThatThrownBy(() -> template.applyTenantFilter(query, Complaint.class, "complaints"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cross-tenant query violation");
    }

    // ── 2. MUTATION & DELETE SCENARIOS ──

    @Test
    @DisplayName("deleteById() / remove() across tenants: Injects societyId into deletion query")
    void remove_unscoped_injectsTenantCriteria() {
        TenantContext.setSocietyId("soc-alpha");

        Query query = new Query(Criteria.where("_id").is("item-123"));
        template.applyTenantFilter(query, Complaint.class, "complaints");

        Document queryObject = query.getQueryObject();
        assertThat(queryObject.get("_id")).isEqualTo("item-123");
        assertThat(queryObject.get("societyId")).isEqualTo("soc-alpha");
    }

    @Test
    @DisplayName("deleteById() / remove() targeting foreign tenant: Throws AccessDeniedException")
    void remove_crossTenantTarget_throwsAccessDenied() {
        TenantContext.setSocietyId("soc-alpha");

        Query query = new Query(Criteria.where("societyId").is("soc-victim"));

        assertThatThrownBy(() -> template.applyTenantFilter(query, Complaint.class, "complaints"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cross-tenant query violation");
    }

    @Test
    @DisplayName("updateFirst() across tenants: Injects societyId into update query")
    void updateFirst_unscoped_injectsTenantCriteria() {
        TenantContext.setSocietyId("soc-alpha");

        Query query = new Query(Criteria.where("status").is("OPEN"));
        template.applyTenantFilter(query, Complaint.class, "complaints");

        Document queryObject = query.getQueryObject();
        assertThat(queryObject.get("societyId")).isEqualTo("soc-alpha");
    }

    @Test
    @DisplayName("updateFirst() targeting foreign tenant: Throws AccessDeniedException")
    void updateFirst_crossTenantTarget_throwsAccessDenied() {
        TenantContext.setSocietyId("soc-alpha");

        Query query = new Query(Criteria.where("societyId").is("soc-other"));

        assertThatThrownBy(() -> template.applyTenantFilter(query, Complaint.class, "complaints"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cross-tenant query violation");
    }

    @Test
    @DisplayName("updateMulti() across tenants: Injects societyId into batch update query")
    void updateMulti_unscoped_injectsTenantCriteria() {
        TenantContext.setSocietyId("soc-alpha");

        Query query = new Query(Criteria.where("status").is("PENDING"));
        template.applyTenantFilter(query, Complaint.class, "complaints");

        Document queryObject = query.getQueryObject();
        assertThat(queryObject.get("societyId")).isEqualTo("soc-alpha");
    }

    @Test
    @DisplayName("updateMulti() targeting foreign tenant: Throws AccessDeniedException")
    void updateMulti_crossTenantTarget_throwsAccessDenied() {
        TenantContext.setSocietyId("soc-alpha");

        Query query = new Query(Criteria.where("societyId").is("soc-hacker"));

        assertThatThrownBy(() -> template.applyTenantFilter(query, Complaint.class, "complaints"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cross-tenant query violation");
    }

    @Test
    @DisplayName("findAndModify() across tenants: Injects societyId into atomic query")
    void findAndModify_unscoped_injectsTenantCriteria() {
        TenantContext.setSocietyId("soc-alpha");

        Query query = new Query(Criteria.where("_id").is("atomic-1"));
        template.applyTenantFilter(query, Complaint.class, "complaints");

        Document queryObject = query.getQueryObject();
        assertThat(queryObject.get("societyId")).isEqualTo("soc-alpha");
    }

    @Test
    @DisplayName("findAndModify() targeting foreign tenant: Throws AccessDeniedException")
    void findAndModify_crossTenantTarget_throwsAccessDenied() {
        TenantContext.setSocietyId("soc-alpha");

        Query query = new Query(Criteria.where("societyId").is("soc-victim"));

        assertThatThrownBy(() -> template.applyTenantFilter(query, Complaint.class, "complaints"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cross-tenant query violation");
    }

    // ── 3. AGGREGATION & PIPELINE ATTACKS ──

    @Test
    @DisplayName("Aggregation with malicious $match: Throws AccessDeniedException")
    void aggregation_withMaliciousMatch_throwsAccessDenied() {
        TenantContext.setSocietyId("soc-alpha");

        Aggregation maliciousAggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("societyId").is("soc-hacker")),
                Aggregation.group("category").count().as("total")
        );

        assertThatThrownBy(() -> template.validateAggregationPipeline(maliciousAggregation, "soc-alpha"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cross-tenant aggregation violation");
    }

    @Test
    @DisplayName("Aggregation with malicious $lookup sub-pipeline: Throws AccessDeniedException")
    void aggregation_withMaliciousLookup_throwsAccessDenied() {
        TenantContext.setSocietyId("soc-alpha");

        Document subMatch = new Document("$match", new Document("societyId", "soc-hacker"));
        Document lookupDoc = new Document("$lookup", new Document("from", "complaints")
                .append("pipeline", List.of(subMatch))
                .append("as", "crossTenantComplaints"));

        Aggregation maliciousLookup = Aggregation.newAggregation(context -> lookupDoc);

        assertThatThrownBy(() -> template.validateAggregationPipeline(maliciousLookup, "soc-alpha"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cross-tenant lookup violation");
    }

    @Test
    @DisplayName("Aggregation pipeline: Prepends $match stage with authenticated societyId")
    void aggregation_prependsMatchStage() {
        TenantContext.setSocietyId("soc-alpha");

        Aggregation agg = Aggregation.newAggregation(
                Aggregation.group("category").count().as("count")
        );

        Aggregation safeAgg = template.injectTenantToAggregation(agg, "soc-alpha");

        Document firstStage = safeAgg.getPipeline().getOperations().get(0)
                .toDocument(Aggregation.DEFAULT_CONTEXT);
        assertThat(firstStage.containsKey("$match")).isTrue();
        Document matchDoc = firstStage.get("$match", Document.class);
        assertThat(matchDoc.get("societyId")).isEqualTo("soc-alpha");
    }

    // ── 4. ENTITY PERSISTENCE MUTATIONS ──

    @Test
    @DisplayName("Entity with forged societyId: Throws AccessDeniedException on save")
    void entity_withForgedSocietyId_throwsAccessDenied() {
        TenantContext.setSocietyId("soc-alpha");

        Complaint forgedComplaint = Complaint.builder()
                .societyId("soc-hacker")
                .title("Exploit attempt")
                .build();

        assertThatThrownBy(() -> template.enforceTenantOnEntity(forgedComplaint, "complaints"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cross-tenant mutation violation");
    }

    @Test
    @DisplayName("Entity with missing societyId: Automatically stamps active tenant")
    void entity_withMissingSocietyId_autoPopulatesTenant() {
        TenantContext.setSocietyId("soc-alpha");

        Complaint newComplaint = Complaint.builder()
                .title("New Water Leakage")
                .build();

        template.enforceTenantOnEntity(newComplaint, "complaints");

        assertThat(newComplaint.getSocietyId()).isEqualTo("soc-alpha");
    }

    // ── 5. ASYNC TASK PROPAGATION & CLEANUP ──

    @Test
    @DisplayName("Async task with tenant propagation: Copies context to worker thread")
    void asyncTask_propagatesTenantContext() throws ExecutionException, InterruptedException, TimeoutException {
        TenantContext.setSocietyId("soc-alpha");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@alpha.com", "pass", List.of()));

        AsyncConfig asyncConfig = new AsyncConfig();
        TaskExecutor executor = asyncConfig.taskExecutor();

        AtomicReference<String> workerTenant = new AtomicReference<>();
        AtomicReference<String> workerAuth = new AtomicReference<>();
        CompletableFuture<Void> future = new CompletableFuture<>();

        executor.execute(() -> {
            workerTenant.set(TenantContext.getSocietyId());
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                workerAuth.set(SecurityContextHolder.getContext().getAuthentication().getName());
            }
            future.complete(null);
        });

        future.get(5, TimeUnit.SECONDS);

        assertThat(workerTenant.get()).isEqualTo("soc-alpha");
        assertThat(workerAuth.get()).isEqualTo("user@alpha.com");
    }

    @Test
    @DisplayName("Async task after context cleanup: Clears thread-local to prevent worker pollution")
    void asyncTask_cleansUpContextAfterExecution() throws ExecutionException, InterruptedException, TimeoutException {
        TenantContext.setSocietyId("soc-beta");

        AsyncConfig asyncConfig = new AsyncConfig();
        TaskExecutor executor = asyncConfig.taskExecutor();

        AtomicReference<Boolean> workerCleanAfterRun = new AtomicReference<>();
        CompletableFuture<Void> future = new CompletableFuture<>();

        executor.execute(() -> {
            // Task executes under soc-beta
            assertThat(TenantContext.getSocietyId()).isEqualTo("soc-beta");
        });

        // Next task on the same pool executor should not retain previous context if submitted without context
        TenantContext.clear();
        executor.execute(() -> {
            workerCleanAfterRun.set(TenantContext.getSocietyId() == null);
            future.complete(null);
        });

        future.get(5, TimeUnit.SECONDS);
        assertThat(workerCleanAfterRun.get()).isTrue();
    }

    // ── 6. SCHEDULED & SCOPED EXECUTION ──

    @Test
    @DisplayName("Scheduled runAsTenant(): Sets and reliably restores tenant context")
    void scheduled_runAsTenant_setsAndRestoresContext() {
        TenantContext.clear();

        TenantContext.runAsTenant("soc-gamma", () -> {
            assertThat(TenantContext.getSocietyId()).isEqualTo("soc-gamma");
        });

        assertThat(TenantContext.getSocietyId()).isNull();
    }

    @Test
    @DisplayName("Scheduled callAsTenant(): Returns computed result and cleans context")
    void scheduled_callAsTenant_returnsResultAndRestoresContext() throws Exception {
        TenantContext.clear();

        String result = TenantContext.callAsTenant("soc-delta", () -> {
            assertThat(TenantContext.getSocietyId()).isEqualTo("soc-delta");
            return "SUCCESS";
        });

        assertThat(result).isEqualTo("SUCCESS");
        assertThat(TenantContext.getSocietyId()).isNull();
    }

    // ── 7. SYSTEM & UNRESTRICTED MODES ──

    @Test
    @DisplayName("System / Super-Admin mode: Allows unrestricted query when context is null")
    void systemMode_noTenantContext_allowsUnrestrictedQuery() {
        TenantContext.clear();

        Query query = new Query(Criteria.where("status").is("OPEN"));
        template.applyTenantFilter(query, Complaint.class, "complaints");

        Document queryObject = query.getQueryObject();
        assertThat(queryObject.containsKey("societyId")).isFalse();
    }

    @Test
    @DisplayName("Non-tenant collection (societies): Exempt from query filtering")
    void nonTenantCollection_societies_isExemptFromFilter() {
        TenantContext.setSocietyId("soc-alpha");

        Query query = new Query(Criteria.where("name").is("Green Valley"));
        template.applyTenantFilter(query, Society.class, "societies");

        Document queryObject = query.getQueryObject();
        assertThat(queryObject.containsKey("societyId")).isFalse();
    }
}
