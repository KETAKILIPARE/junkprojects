package com.apigateway.filter;

import com.apigateway.domain.RequestLog;
import com.apigateway.domain.RouteConfig;
import com.apigateway.exception.RateLimitExceededException;
import com.apigateway.exception.RouteNotFoundException;
import com.apigateway.repository.RequestLogRepository;
import com.apigateway.service.GatewayService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

@Component
@Order(1)
@RequiredArgsConstructor
public class GatewayFilter implements Filter {

    private final GatewayService gatewayService;
    private final RequestLogRepository requestLogRepository;
    private final RestTemplate restTemplate;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI();

        // Let management endpoints through
        if (path.startsWith("/api/metrics") || path.startsWith("/api/routes")) {
            chain.doFilter(req, res);
            return;
        }

        long start = System.currentTimeMillis();
        String clientIp = request.getRemoteAddr();
        String method = request.getMethod();

        try {
            RouteConfig route = gatewayService.resolveRoute(path, clientIp);

            // Check JWT if route requires auth
            if (route.isRequiresAuth()) {
                String header = request.getHeader("Authorization");
                if (header == null || !header.startsWith("Bearer ") || !isValidJwt(header.substring(7))) {
                    writeError(response, 401, "Unauthorized");
                    logRequest(method, path, clientIp, 401, start);
                    return;
                }
            }

            // Forward request to target
            String targetUrl = route.getTargetUrl() + path;
            HttpHeaders headers = copyHeaders(request);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> targetResponse = restTemplate.exchange(
                    targetUrl, HttpMethod.valueOf(method), entity, String.class);

            response.setStatus(targetResponse.getStatusCode().value());
            response.setContentType("application/json");
            if (targetResponse.getBody() != null) {
                response.getWriter().write(targetResponse.getBody());
            }

            logRequest(method, path, clientIp, targetResponse.getStatusCode().value(), start);

        } catch (RouteNotFoundException ex) {
            writeError(response, 404, "No route found for: " + path);
            logRequest(method, path, clientIp, 404, start);
        } catch (RateLimitExceededException ex) {
            writeError(response, 429, "Rate limit exceeded");
            logRequest(method, path, clientIp, 429, start);
        } catch (Exception ex) {
            writeError(response, 502, "Bad gateway: " + ex.getMessage());
            logRequest(method, path, clientIp, 502, start);
        }
    }

    private HttpHeaders copyHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            Collections.list(headerNames).forEach(name ->
                    headers.set(name, request.getHeader(name)));
        }
        return headers;
    }

    private boolean isValidJwt(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    private void logRequest(String method, String path, String clientIp, int status, long start) {
        long latency = System.currentTimeMillis() - start;
        requestLogRepository.save(new RequestLog(method, path, clientIp, status, latency));
    }
}
