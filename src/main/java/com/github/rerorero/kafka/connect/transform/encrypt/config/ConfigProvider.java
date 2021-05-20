package com.github.rerorero.kafka.connect.transform.encrypt.config;

import java.util.Map;

public interface ConfigProvider{
    Config NewConfig(Map<String, ?> props);
}
