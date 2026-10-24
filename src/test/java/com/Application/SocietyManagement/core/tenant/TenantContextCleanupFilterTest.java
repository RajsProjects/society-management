package com.Application.SocietyManagement.core.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TenantContextCleanupFilterTest {

    private TenantContextCleanupFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new TenantContextCleanupFilter();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Clears stale ThreadLocal before starting request and clears again upon successful completion")
    void clearsBeforeAndAfterRequest() throws ServletException, IOException {
        TenantContext.setSocietyId("stale-society-id");

        AtomicBoolean verifiedCleanInsideChain = new AtomicBoolean(false);

        doAnswer(invocation -> {
            verifiedCleanInsideChain.set(TenantContext.getSocietyId() == null);
            TenantContext.setSocietyId("new-society-id");
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(verifiedCleanInsideChain.get()).isTrue();
        assertThat(TenantContext.getSocietyId()).isNull();
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Guarantees TenantContext.clear() runs even when filterChain throws ServletException")
    void clearsOnServletException() throws ServletException, IOException {
        doAnswer(invocation -> {
            TenantContext.setSocietyId("leaked-society-id");
            throw new ServletException("Simulated filterChain exception");
        }).when(filterChain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(ServletException.class)
                .hasMessageContaining("Simulated filterChain exception");

        assertThat(TenantContext.getSocietyId()).isNull();
    }

    @Test
    @DisplayName("Guarantees TenantContext.clear() runs even when filterChain throws RuntimeException")
    void clearsOnRuntimeException() throws ServletException, IOException {
        doAnswer(invocation -> {
            TenantContext.setSocietyId("leaked-society-id");
            throw new RuntimeException("Unexpected runtime error");
        }).when(filterChain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unexpected runtime error");

        assertThat(TenantContext.getSocietyId()).isNull();
    }
}
