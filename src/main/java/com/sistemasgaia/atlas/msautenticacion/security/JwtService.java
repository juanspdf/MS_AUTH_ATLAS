package com.sistemasgaia.atlas.msautenticacion.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Servicio para generacion, validacion y extraccion de datos de tokens JWT.
 *
 * Algoritmo: RS256 (RSA de 2048 bits, asimetrico)
 * - La llave privada firma el token.
 * - La llave publica verifica la firma.
 *
 * Claims incluidos en el token:
 * - sub (username)
 * - idUsuario (UUID)
 * - rol (String)
 * - politicas (List<String>)
 */
@Service
public class JwtService {

    @Value("${jwt.private-key}")
    private String privateKeyBase64;

    @Value("${jwt.public-key}")
    private String publicKeyBase64;

    @Value("${jwt.expiration}")
    private long expirationMs;

    public String generarToken(UUID idUsuario, String username, String rol, List<String> politicas) {
        return Jwts.builder()
                .subject(username)
                .claims(Map.of(
                        "idUsuario", idUsuario.toString(),
                        "rol", rol,
                        "politicas", politicas
                ))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    public String extraerUsername(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    public UUID extraerIdUsuario(String token) {
        return UUID.fromString(extraerClaim(token, claims -> claims.get("idUsuario", String.class)));
    }

    public String extraerRol(String token) {
        return extraerClaim(token, claims -> claims.get("rol", String.class));
    }

    public List<String> extraerPoliticas(String token) {
        List<?> rawList = extraerClaim(token, claims -> claims.get("politicas", List.class));
        if (rawList == null) return List.of();
        return rawList.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }

    public boolean validarToken(String token, String username) {
        final String tokenUsername = extraerUsername(token);
        return tokenUsername.equals(username) && !isTokenExpirado(token);
    }

    public LocalDateTime extraerExpiracion(String token) {
        Date expiration = extraerClaim(token, Claims::getExpiration);
        return expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private <T> T extraerClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extraerTodosClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extraerTodosClaims(String token) {
        return Jwts.parser()
                .verifyWith(getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpirado(String token) {
        return extraerClaim(token, Claims::getExpiration).before(new Date());
    }

    private PrivateKey getPrivateKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(privateKeyBase64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Error al cargar la llave privada JWT", e);
        }
    }

    private PublicKey getPublicKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(publicKeyBase64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Error al cargar la llave publica JWT", e);
        }
    }
}
