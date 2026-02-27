package com.nsmall.ns_creative_studio_bo.common.security;

import com.nsmall.ns_creative_studio_bo.common.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 발급 및 검증 컴포넌트
 *
 * <pre>
 * Access Token 생성:
 *   - subject: loginId
 *   - claim "role": ADMIN / MANAGER / USER
 *   - 유효시간: JwtProperties.expiration (초 단위)
 *   - 알고리즘: HMAC-SHA256
 * </pre>
 *
 * @author jjhoo8936
 * @since 2026-02-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        secretKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Access Token 생성
     *
     * @param loginId 사용자 loginId (JWT subject)
     * @param role    사용자 권한 (ADMIN / MANAGER / USER)
     * @return 서명된 JWT 문자열
     */
    public String generateToken(String loginId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpiration() * 1000L);

        return Jwts.builder()
                .subject(loginId)
                .claim(CLAIM_ROLE, role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰에서 loginId(subject) 추출
     *
     * @param token JWT 문자열
     * @return loginId
     */
    public String getLoginId(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 토큰 유효성 검증
     *
     * @param token JWT 문자열
     * @return 유효하면 {@code true}, 만료·위조·파싱 실패 시 {@code false}
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("유효하지 않은 JWT 토큰: {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
