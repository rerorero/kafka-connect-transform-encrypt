package com.github.rerorero.kafka.connect.transform.encrypt.config;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class OneOfValidator<T> implements ConfigDef.Validator {
    private final List<T> variables;

    OneOfValidator(T... vars) {
        this.variables = Arrays.asList(vars);
    }

    @Override
    public void ensureValid(String name, Object value) {
        if (!variables.contains(value)) {
            throw new ConfigException(name, value, "Not valid: should be one of " +
                    String.join(",", variables.stream().map(v -> v.toString()).collect(Collectors.toList())));
        }
    }
}
