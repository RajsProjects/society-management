package com.Application.SocietyManagement.core.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class JoinCodeGenerator {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 7;
    private static final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder("SOC-");
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString(); // e.g. SOC-KD8X72A
    }
}