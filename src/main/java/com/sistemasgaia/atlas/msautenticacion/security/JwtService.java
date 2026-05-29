package com.sistemasgaia.atlas.msautenticacion.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Servicio para generación, validación y extracción de datos de tokens JWT.
 *
 * El token incluye:
 * - idUsuario (UUID)
 * - username (subject)
 * - rol (String)
 * - politicas (List<String>)
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationMs;

    /**
     * Genera un token JWT con los claims del usuario.
     */
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
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrae el username (subject) del token.
     */
    public String extraerUsername(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    /**
     * Extrae el ID del usuario del token.
     */
    public UUID extraerIdUsuario(String token) {
        return UUID.fromString(extraerClaim(token, claims -> claims.get("idUsuario", String.class)));
    }

    /**
     * Extrae el rol del token.
     */
    public String extraerRol(String token) {
        return extraerClaim(token, claims -> claims.get("rol", String.class));
    }

    /**
     * Extrae las políticas del token.
     */
    @SuppressWarnings("unchecked")
    public List<String> extraerPoliticas(String token) {
        return extraerClaim(token, claims -> claims.get("politicas", List.class));
    }

    /**
     * Valida que el token sea válido para el username dado.
     */
    public boolean validarToken(String token, String username) {
        final String tokenUsername = extraerUsername(token);
        return tokenUsername.equals(username) && !isTokenExpirado(token);
    }

    /**
     * Extrae la fecha de expiración del token como LocalDateTime.
     * Se utiliza en el proceso de logout para registrar cuándo expira el token invalidado.
     */
    public LocalDateTime extraerExpiracion(String token) {
        Date expiration = extraerClaim(token, Claims::getExpiration);
        return expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Extrae un claim específico del token.
     */
    private <T> T extraerClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extraerTodosClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrae todos los claims del token.
     */
    private Claims extraerTodosClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Verifica si el token ha expirado.
     */
    private boolean isTokenExpirado(String token) {
        return extraerClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Obtiene la clave de firma HMAC desde el secret configurado.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
