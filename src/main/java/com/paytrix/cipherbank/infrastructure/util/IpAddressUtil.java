package com.paytrix.cipherbank.infrastructure.util;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Utility class to extract the real client IP address from HTTP requests,
 * especially when behind reverse proxies, load balancers, or CDNs.
 */
public class IpAddressUtil {

    private static final Logger logger = LoggerFactory.getLogger(IpAddressUtil.class);

    // Common proxy headers in order of preference
    private static final List<String> IP_HEADER_CANDIDATES = Arrays.asList(
            "X-Forwarded-For",      // Standard header used by most proxies
            "X-Real-IP",            // Nginx proxy
            "Proxy-Client-IP",      // Apache
            "WL-Proxy-Client-IP",   // WebLogic
            "HTTP_X_FORWARDED_FOR", // Alternate format
            "HTTP_X_FORWARDED",     // Alternate format
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    );

    // Known localhost and private network addresses that should be filtered
    private static final List<String> IGNORED_IPS = Arrays.asList(
            "127.0.0.1",
            "0:0:0:0:0:0:0:1",
            "::1",
            "localhost"
    );

    /**
     * Extract the real client IP address from the HTTP request.
     * Checks standard proxy headers and handles comma-separated IPs.
     *
     * @param request HttpServletRequest
     * @return Client IP address
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            logger.warn("Request is null, cannot extract IP address");
            return "unknown";
        }

        // Try to get IP from proxy headers
        for (String header : IP_HEADER_CANDIDATES) {
            String ip = request.getHeader(header);
            if (isValidIp(ip)) {
                // X-Forwarded-For can contain multiple IPs: client, proxy1, proxy2
                // The first IP is the original client
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }

                if (isValidIp(ip) && !isLocalOrPrivateIp(ip)) {
                    logger.debug("Client IP extracted from header '{}': {}", header, ip);
                    return ip;
                }
            }
        }

        // Fall back to remote address if no valid header found
        String remoteAddr = request.getRemoteAddr();
        logger.debug("Client IP extracted from RemoteAddr: {}", remoteAddr);
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    /**
     * Check if the IP string is valid (not null, not empty, not "unknown")
     */
    private static boolean isValidIp(String ip) {
        return ip != null
                && !ip.trim().isEmpty()
                && !"unknown".equalsIgnoreCase(ip.trim());
    }

    /**
     * Check if IP is localhost or should be ignored
     * (This is a simple check - in production you might want more sophisticated logic)
     */
    private static boolean isLocalOrPrivateIp(String ip) {
        if (ip == null) return true;

        String trimmedIp = ip.trim();

        // Check against known localhost addresses
        if (IGNORED_IPS.contains(trimmedIp)) {
            return true;
        }

        // Optional: You can add logic here to filter private IP ranges
        // 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
        // For AWS EC2, you might want to allow these for internal services

        return false;
    }

    /**
     * Check if an IP belongs to a private network range.
     * Useful if you want to filter out internal network IPs.
     */
    public static boolean isPrivateNetwork(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) {
                return false; // Not IPv4
            }

            int firstOctet = Integer.parseInt(parts[0]);
            int secondOctet = Integer.parseInt(parts[1]);

            // 10.0.0.0 - 10.255.255.255 (Class A)
            if (firstOctet == 10) {
                return true;
            }

            // 172.16.0.0 - 172.31.255.255 (Class B)
            if (firstOctet == 172 && secondOctet >= 16 && secondOctet <= 31) {
                return true;
            }

            // 192.168.0.0 - 192.168.255.255 (Class C)
            if (firstOctet == 192 && secondOctet == 168) {
                return true;
            }

            // 127.0.0.0 - 127.255.255.255 (Loopback)
            if (firstOctet == 127) {
                return true;
            }

            return false;
        } catch (NumberFormatException e) {
            logger.warn("Invalid IP format: {}", ip);
            return false;
        }
    }

    /**
     * Get all IP-related headers for debugging purposes
     */
    public static String getAllIpHeaders(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("IP Headers Debug:\n");

        for (String header : IP_HEADER_CANDIDATES) {
            String value = request.getHeader(header);
            if (value != null) {
                sb.append(String.format("  %s: %s\n", header, value));
            }
        }

        sb.append(String.format("  RemoteAddr: %s\n", request.getRemoteAddr()));
        sb.append(String.format("  RemoteHost: %s\n", request.getRemoteHost()));
        sb.append(String.format("  RemotePort: %s\n", request.getRemotePort()));

        return sb.toString();
    }
}