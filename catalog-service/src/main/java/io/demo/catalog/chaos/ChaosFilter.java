package io.demo.catalog.chaos;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Adds artificial latency and random 503s to /api/** while chaos is switched on. */
@Component
public class ChaosFilter extends OncePerRequestFilter {

    private final ChaosState chaos;

    public ChaosFilter(ChaosState chaos) {
        this.chaos = chaos;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long latency = chaos.latencyMs();
        if (latency > 0) {
            try {
                Thread.sleep(latency);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (chaos.errorRate() > 0 && ThreadLocalRandom.current().nextDouble() < chaos.errorRate()) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("text/plain");
            response.getWriter().write("chaos: injected failure");
            return;
        }
        chain.doFilter(request, response);
    }
}
