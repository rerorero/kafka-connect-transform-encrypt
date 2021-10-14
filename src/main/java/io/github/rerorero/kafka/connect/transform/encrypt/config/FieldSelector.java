package io.github.rerorero.kafka.connect.transform.encrypt.config;

import io.github.rerorero.kafka.jsonpath.JsonPath;
import org.apache.kafka.connect.data.Struct;

import java.util.HashMap;
import java.util.Map;

public class FieldSelector {
    public Map<String, JsonPath.Getter<Map<String, Object>>> mapGetters = new HashMap<>();
    public Map<String, JsonPath.Updater<Map<String, Object>>> mapUpdaters = new HashMap<>();
    public Map<String, JsonPath.Getter<Struct>> structGetters = new HashMap<>();
    public Map<String, JsonPath.Updater<Struct>> structUpdaters = new HashMap<>();
}
