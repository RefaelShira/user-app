package com.example.user.domain;

import com.example.user.exceptions.RequiredParamsException;
import com.example.user.exceptions.ShortPasswordException;
import com.example.user.exceptions.WeakPasswordException;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    public void validate(String email, String password) {
        if (email == null || password == null) {
            throw new RequiredParamsException("Email/Password required");
        }
        boolean isExample = email.toLowerCase().endsWith("@example.com");
        int minLen = isExample ? 12 : 8;

        if (password.length() < minLen) {
            throw new ShortPasswordException("Password too short");
        }
        int categories = 0;
        if (password.matches(".*[a-z].*")) categories++;
        if (password.matches(".*[A-Z].*")) categories++;
        if (password.matches(".*\\d.*"))   categories++;
        if (password.matches(".*[^A-Za-z0-9].*")) categories++;
        if (categories < 3) {
            throw new WeakPasswordException("Weak password");
        }
    }
}
