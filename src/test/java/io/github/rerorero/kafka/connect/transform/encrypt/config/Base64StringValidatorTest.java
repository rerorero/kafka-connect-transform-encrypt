package io.github.rerorero.kafka.connect.transform.encrypt.config;


import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base64StringValidatorTest {
    @Test
    public void testBase64StringValidator() {
        Base64StringValidator v = Base64StringValidator.singleton;
        assertDoesNotThrow(() -> v.ensureValid("a", "dGVzdA=="));
        assertThrows(ConfigException.class, () -> v.ensureValid("b", "ねむい"));
    }
}