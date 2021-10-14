package io.github.rerorero.kafka.connect.transform.encrypt.condition;

import io.github.rerorero.kafka.jsonpath.JsonPath;
import io.github.rerorero.kafka.jsonpath.MapSupport;
import io.github.rerorero.kafka.jsonpath.StructSupport;
import org.apache.kafka.connect.data.Struct;

import java.util.Map;

public class Conditions {
    public final Condition<Map<String, Object>> mapCondition;
    public final Condition<Struct> structCondition;

    private final JsonPath.Getter<Map<String, Object>> mapGetter;
    private final JsonPath.Getter<Struct> structGetter;

    public Conditions(String jsonPath, String comparison) {
        this.mapGetter = MapSupport.newGetter(jsonPath);
        this.mapCondition = r -> checkCondition(mapGetter, r, comparison);
        this.structGetter = StructSupport.newGetter(jsonPath);
        this.structCondition = r -> checkCondition(structGetter, r, comparison);
    }

    // returns empty Conditions which accepts all records.
    public Conditions() {
        this.mapGetter = null;
        this.mapCondition = r -> true;
        this.structGetter = null;
        this.structCondition = r -> true;
    }

    private <R> boolean checkCondition(JsonPath.Getter<R> getter, R record, String comparison) {
        Map<String, Object> values = getter.run(record);
        if (values.isEmpty()) {
            return false;
        }

        for (Object value : values.values()) {
            if (value.toString().equals(comparison)) {
                return true;
            }
        }
        return false;
    }
}
