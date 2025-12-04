package com.paytrix.cipherbank.infrastructure.adapter.in.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * TEMPORARY DEBUG CONTROLLER
 * This controller helps debug authentication issues by showing all request headers
 * DELETE THIS FILE AFTER DEBUGGING!
 */
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    /**
     * Test endpoint to see what headers the backend receives
     * Access: http://cipher.thepaytrix.com/api/debug/headers
     */
    @GetMapping("/headers")
    public ResponseEntity<Map<String, Object>> showHeaders(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        // Collect all headers
        Map<String, String> headers = new HashMap<>();
        Collections.list(request.getHeaderNames()).forEach(headerName ->
                headers.put(headerName, request.getHeader(headerName))
        );

        result.put("headers", headers);
        result.put("method", request.getMethod());
        result.put("requestURI", request.getRequestURI());
        result.put("remoteAddr", request.getRemoteAddr());

        // Check for Authorization header specifically
        String authHeader = request.getHeader("Authorization");
        result.put("hasAuthHeader", authHeader != null);
        if (authHeader != null) {
            result.put("authHeaderPreview", authHeader.substring(0, Math.min(30, authHeader.length())) + "...");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Test endpoint to check if JWT token is valid
     * POST to: http://cipher.thepaytrix.com/api/debug/test-auth
     * Include Authorization: Bearer <token> header
     */
    @PostMapping("/test-auth")
    public ResponseEntity<Map<String, Object>> testAuth(
            HttpServletRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Map<String, Object> result = new HashMap<>();

        result.put("authHeaderReceived", authHeader != null);
        result.put("remoteAddr", request.getRemoteAddr());

        if (authHeader != null) {
            result.put("startsWithBearer", authHeader.startsWith("Bearer "));
            result.put("tokenLength", authHeader.length());
            result.put("tokenPreview", authHeader.substring(0, Math.min(40, authHeader.length())) + "...");
        } else {
            result.put("error", "No Authorization header received");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Echo endpoint - returns whatever you send
     * POST to: http://cipher.thepaytrix.com/api/debug/echo
     */
    @PostMapping("/echo")
    public ResponseEntity<Map<String, Object>> echo(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Echo successful");
        result.put("receivedBody", body);
        return ResponseEntity.ok(result);
    }
}
