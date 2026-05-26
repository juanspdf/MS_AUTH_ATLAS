package com.sistemasgaia.atlas.msautenticacion.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utilidad para generar hashes BCrypt.
 * Ejecutar como clase main para obtener hashes de contraseñas.
 *
 * Uso: java BcryptUtil
 */
public class BcryptUtil {

    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "admin123";
        String hash = encoder.encode(rawPassword);
        System.out.println("Password: " + rawPassword);
        System.out.println("Hash:     " + hash);
        System.out.println("Matches:  " + encoder.matches(rawPassword, hash));
    }
}
