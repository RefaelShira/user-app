package com.example.user.domain;

import com.example.user.exceptions.RequiredParamsException;
import com.example.user.exceptions.ShortPasswordException;
import com.example.user.exceptions.WeakPasswordException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;
public class PasswordPolicyTest {
    PasswordPolicy policy = new PasswordPolicy();

    // ---------- nulls ----------

    @Test
    @DisplayName("emailNull")
    void emailNullTest() {
        assertThatThrownBy(() -> policy.validate(null, "Abcdef12!"))
                .isInstanceOf(RequiredParamsException.class);
    }

    @Test
    @DisplayName("passwordNull")
    void passwordNullTest() {
        assertThatThrownBy(() -> policy.validate("israel@example.com", null))
                .isInstanceOf(RequiredParamsException.class);
    }


    @Test
    @DisplayName("shortPassword")
    void shortPasswordTest() {
        assertThatThrownBy(() -> policy.validate("israel@example.com", "Abc1!def"))
                .isInstanceOf(ShortPasswordException.class);
    }

    @Test
    @DisplayName("strongPassword")
    void strongPasswordTest() {
        assertThatCode(() -> policy.validate("user@example.com", "Abcdef1!Ghij"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("veryShortPassword")
    void veryShortPasswordTest() {
        assertThatThrownBy(() -> policy.validate("user@other.com", "Abc1!de"))
                .isInstanceOf(ShortPasswordException.class);
    }

    @Test
    @DisplayName("finePassword")
    void finePasswordTest() {
        assertThatCode(() -> policy.validate("user@other.com", "Abc1!def"))
                .doesNotThrowAnyException();
    }


    @ParameterizedTest(name = "WeakPasswordException")
    @ValueSource(strings = {
            "abcdefgh",
            "ABCDEFGH",
            "12345678",
            "abcdEFGH",
            "abcd1234",
            "ABCD1234",
            "abcd!@#$",
            "ABCD!@#$",
            "1234!@#$"
    })
    void weakPasswords_throw(String pwd) {
        assertThatThrownBy(() -> policy.validate("user@other.com", pwd))
                .isInstanceOf(WeakPasswordException.class);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {
            "Abcd1234",
            "Abcd!@#$",
            "ABCdef!@",
            "ABC123!@",
            "abc123!@",
            "Abc1!def"
    })
    void strongEnough_ok(String pwd) {
        assertThatCode(() -> policy.validate("user@other.com", pwd))
                .doesNotThrowAnyException();
    }
    

}
