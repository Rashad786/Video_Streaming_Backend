package com.rashad.Video_Streaming_Backend.service;

import com.rashad.Video_Streaming_Backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JWTService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}")      // 24 hours default
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration:604800000}") // 7 days default
    private long refreshExpiration;

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        return buildToken(claims, user.getUserName(), jwtExpiration);
     }

    public String generateRefreshToken(User user) {
        return buildToken(new HashMap<>(), user.getUserName(), refreshExpiration);
    }

    private String buildToken(Map<String, Object> claims, String subject, long jwtExpiration) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis()+jwtExpiration))
                .signWith(getSignKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSignKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }


    public boolean isTokenValid(String token, String userName) {
        try{
            return extractUsername(token).equals(userName) && !isTokenExpired(token);
        }catch (JwtException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ── Extract claims ────────────────────────────────────────────────────────
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
