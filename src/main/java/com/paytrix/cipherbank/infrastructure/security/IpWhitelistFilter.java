package com.paytrix.cipherbank.infrastructure.security;

import com.paytrix.cipherbank.application.service.IpWhitelistService;
import com.paytrix.cipherbank.infrastructure.util.IpAddressUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * IP Whitelist Filter to restrict access based on client IP addresses.
 * Properly handles reverse proxies and load balancers to get real client IP.
 */
@Component
public class IpWhitelistFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(IpWhitelistFilter.class);

    private final IpWhitelistService ipWhitelistService;

    @Value("${security.ip-whitelist.enabled:true}")
    private boolean ipWhitelistEnabled;

    @Value("${security.ip-whitelist.debug:false}")
    private boolean debugMode;

    public IpWhitelistFilter(IpWhitelistService ipWhitelistService) {
        this.ipWhitelistService = ipWhitelistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Skip IP whitelist check if disabled
        if (!ipWhitelistEnabled) {
            logger.debug("IP whitelist filter is disabled");
            filterChain.doFilter(request, response);
            return;
        }

        // Get the real client IP address
        String clientIp = IpAddressUtil.getClientIpAddress(request);

        // Debug logging
        if (debugMode) {
            logger.info("IP Whitelist Check - Request URI: {}", request.getRequestURI());
            logger.info("Extracted Client IP: {}", clientIp);
            logger.info(IpAddressUtil.getAllIpHeaders(request));
        }

        // Get whitelisted IPs
        Set<String> whitelistedIps = ipWhitelistService.getActiveIps();

        if (debugMode) {
            logger.info("Whitelisted IPs: {}", whitelistedIps);
        }

        // Check if client IP is whitelisted
        if (!whitelistedIps.contains(clientIp)) {
            logger.warn("Access denied for IP: {} - Request URI: {} - Method: {}",
                    clientIp, request.getRequestURI(), request.getMethod());

            if (debugMode) {
                logger.warn("Client IP '{}' not found in whitelist: {}", clientIp, whitelistedIps);
            }

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            String jsonResponse = String.format(
                    "{\"error\": \"Access Denied\", \"message\": \"IP address not whitelisted\", \"ip\": \"%s\"}",
                    clientIp
            );

            response.getWriter().write(jsonResponse);
            return;
        }

        logger.debug("Access granted for IP: {} - Request URI: {}", clientIp, request.getRequestURI());
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // You can add paths that should bypass IP whitelist here
        String path = request.getRequestURI();

        // Example: Skip IP check for health check endpoints
        // return path.startsWith("/actuator/health") || path.startsWith("/health");

        return false;
    }
}