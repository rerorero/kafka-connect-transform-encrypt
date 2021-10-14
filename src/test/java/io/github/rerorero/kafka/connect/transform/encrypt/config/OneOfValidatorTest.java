package io.github.rerorero.kafka.connect.transform.encrypt.config;

import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OneOfValidatorTest {
    @Test
    public void testOneOfValidator() {
        OneOfValidator v = new OneOfValidator<>("one", "two");
        assertDoesNotThrow(() -> v.ensureValid("1", "one"));
        assertDoesNotThrow(() -> v.ensureValid("2", "two"));
        assertThrows(ConfigException.class, () ->v.ensureValid("3", "three"));
    }
}