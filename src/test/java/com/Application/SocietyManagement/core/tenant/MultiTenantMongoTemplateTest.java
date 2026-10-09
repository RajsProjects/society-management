package com.Application.SocietyManagement.core.tenant;

import com.Application.SocietyManagement.complaint.entity.Complaint;
import com.Application.SocietyManagement.society.entity.Society;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("MultiTenantMongoTemplate Isolation Tests")
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
    }

    @Test
    @DisplayName("Tenant filter automatically injects societyId on un-scoped query")
    void applyTenantFilter_injectsTenantCriteria() {
        TenantContext.setSocietyId("soc-alpha");

        Query query = new Query(Criteria.where("status").is("OPEN"));
        // Calling count or find triggers applyTenantFilter internally
        try {
            template.count(query, Complaint.class, "complaints");
        } catch (Exception ignored) {
            // Factory is mocked, we only test query mutation
        }

        Document queryObject = query.getQueryObject();
        assertThat(queryObject.get("societyId")).isEqualTo("soc-alpha");
        assertThat(queryObject.get("status")).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("Throws AccessDeniedException when query targets a different societyId")
    void applyTenantFilter_crossTenantTarget_throwsAccessDenied() {
        TenantContext.setSocietyId("soc-alpha");

        Query query = new Query(Criteria.where("societyId").is("soc-hacker"));

        assertThatThrownBy(() -> template.count(query, Complaint.class, "complaints"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cross-tenant query violation");
    }

    @Test
    @DisplayName("Exempts societies collection from tenant filter")
    void applyTenantFilter_exemptsSocietiesCollection() {
        TenantContext.setSocietyId("soc-alpha");

        Query query = new Query(Criteria.where("code").is("SOC-123"));
        try {
            template.count(query, Society.class, "societies");
        } catch (Exception ignored) {
        }

        Document queryObject = query.getQueryObject();
        assertThat(queryObject.containsKey("societyId")).isFalse();
    }

    @Test
    @DisplayName("Does not modify query when TenantContext is null (system mode)")
    void applyTenantFilter_systemMode_doesNotInject() {
        TenantContext.clear();

        Query query = new Query(Criteria.where("status").is("OPEN"));
        try {
            template.count(query, Complaint.class, "complaints");
        } catch (Exception ignored) {
        }

        Document queryObject = query.getQueryObject();
        assertThat(queryObject.containsKey("societyId")).isFalse();
    }
}
