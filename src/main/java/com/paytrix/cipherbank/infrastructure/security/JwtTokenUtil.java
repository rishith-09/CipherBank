package com.paytrix.cipherbank.infrastructure.security;

import com.paytrix.cipherbank.infrastructure.config.JwtProperties;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;

import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.security.Role;
import com.paytrix.cipherbank.infrastructure.adapter.out.persistence.entity.security.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtTokenUtil {

    @Autowired
    private JwtProperties jwtProperties;

    public String generateTokenFromUser(User user) {

        try {

            List<String> roles = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList());

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(user.getUsername())
                    .claim("roles", roles)
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + jwtProperties.getExpiration()))
                    .build();

            JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A256GCM)
                    .contentType("JWT")
                    .build();

            EncryptedJWT jwt = new EncryptedJWT(header, claims);

            DirectEncrypter encrypter = new DirectEncrypter(jwtProperties.getEncryptionSecret().getBytes(StandardCharsets.UTF_8));
            jwt.encrypt(encrypter);

            return jwt.serialize();
        } catch (Exception e) {
            throw new RuntimeException("Error generating JWE token", e);
        }
    }

    private JWTClaimsSet getClaims(String token) {

        try {

            EncryptedJWT jwt = EncryptedJWT.parse(token);
            DirectDecrypter decrypter = new DirectDecrypter(jwtProperties.getEncryptionSecret().getBytes(StandardCharsets.UTF_8));
            jwt.decrypt(decrypter);

            return jwt.getJWTClaimsSet();
        } catch (Exception e) {
            throw new RuntimeException("Invalid or expired JWE token", e);
        }
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {

        Object roles = getClaims(token).getClaim("roles");

        if (roles instanceof List) {
            return ((List<?>) roles).stream().map(Object::toString).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    public boolean isTokenExpired(String token) {

        Date exp = getClaims(token).getExpirationTime();
        return exp.before(new Date());
    }
}
