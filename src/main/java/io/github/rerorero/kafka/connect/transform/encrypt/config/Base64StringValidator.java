package io.github.rerorero.kafka.connect.transform.encrypt.config;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;

import java.util.Base64;

public class Base64StringValidator implements ConfigDef.Validator {
    public static final Base64StringValidator singleton = new Base64StringValidator();

    @Override
    public void ensureValid(String name, Object value) {
        if (value == null) {
            return;
        }
        try {
            Base64.getDecoder().decode((String) value);
        } catch(IllegalArgumentException e) {
            throw new ConfigException(name, value, "Failed to parse as Base64 String: " + e.getMessage());
        }
    }
}
