package com.guidelinex.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SearchRequestLoggingFilter provides system observability and protection.
 * 
 * Hardening Measures:
 * - Logs query performance (latency tracking)
 * - Logs search patterns (clinical analysis)
 * - Implements basic rate limiting (50 RPM/IP) to protect the database
 */
@Component
@Slf4j
public class SearchRequestLoggingFilter implements Filter {

    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public SearchRequestLoggingFilter() {
        scheduler.scheduleAtFixedRate(requestCounts::clear, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (httpRequest.getRequestURI().startsWith("/search")) {
            String ip = httpRequest.getRemoteAddr();
            int count = requestCounts.computeIfAbsent(ip, k -> new AtomicInteger(0))
                    .incrementAndGet();

            if (count > 50) { // Basic rate limit: 50 requests per minute
                log.warn("RATE_LIMIT | IP: {} | Count: {}", ip, count);
                httpResponse.setStatus(429);
                httpResponse.getWriter().write("Too Many Requests - Rate limit exceeded");
                return;
            }

            long startTime = System.currentTimeMillis();

            try {
                chain.doFilter(request, response);
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                String query = httpRequest.getParameter("q");
                String types = httpRequest.getParameter("type");
                int status = httpResponse.getStatus();

                log.info("SEARCH_LOG | Status: {} | Duration: {}ms | Q: '{}' | Types: {} | IP: {}",
                        status, duration, query, types, ip);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        scheduler.shutdown();
    }
}
