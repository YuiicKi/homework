package com.ruangong.service;

import com.ruangong.config.JwtProperties;
import com.ruangong.model.JwtPayload;
import com.ruangong.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final int MIN_HMAC_KEY_BYTES = 32;

    private final JwtProperties properties;
    private final UserRepository userRepository;
    private Key signingKey;

    public JwtService(JwtProperties properties, UserRepository userRepository) {
        this.properties = properties;
        this.userRepository = userRepository;
    }

    @PostConstruct
    void init() {
        byte[] keyBytes;
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret must be configured");
        }

        // Support raw text or base64 encoded secrets
        if (secret.startsWith("base64:")) {
            keyBytes = Decoders.BASE64.decode(secret.substring("base64:".length()));
        } else {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }

        if (keyBytes.length < MIN_HMAC_KEY_BYTES) {
            log.warn("JWT secret is {} bytes; deriving SHA-256 digest so HS256 requirements are met.",
                keyBytes.length);
            keyBytes = strengthenKey(keyBytes);
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    private byte[] strengthenKey(byte[] keyBytes) {
        try {
            // SHA-256 digest always returns 256 bits, satisfying HS256's key requirements.
            return MessageDigest.getInstance("SHA-256").digest(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    public String generateToken(Long userId, List<String> roles, long tokenVersion) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.getExpiresInSeconds());
        return Jwts.builder()
            .setSubject(userId.toString())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiresAt))
            .claim("roles", roles)
            .claim("ver", tokenVersion)
            .signWith(signingKey, SignatureAlgorithm.HS256)
            .compact();
    }

    public JwtPayload parseToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(signingKey)
            .build()
            .parseClaimsJws(token)
            .getBody();

        Long userId = Long.parseLong(claims.getSubject());
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        Number version = claims.get("ver", Number.class);
        if (version == null) {
            throw new IllegalStateException("Token 缺少版本信息");
        }

        long tokenVersion = version.longValue();
        return userRepository.findById(userId)
            .map(user -> {
                long currentVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0L;
                if (currentVersion != tokenVersion) {
                    throw new IllegalStateException("Token 已被撤销");
                }
                return new JwtPayload(userId, roles, tokenVersion);
            })
            .orElseThrow(() -> new IllegalStateException("用户不存在"));
    }
}
