package com.Application.SocietyManagement.core.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PepperedPasswordEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder bcrypt;
    private final String pepper;

    public PepperedPasswordEncoder(String pepper) {
        this.bcrypt = new BCryptPasswordEncoder(12); // strength 12 = good balance of security vs speed
        this.pepper = pepper;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return bcrypt.encode(rawPassword + pepper);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return bcrypt.matches(rawPassword + pepper, encodedPassword);
    }
}