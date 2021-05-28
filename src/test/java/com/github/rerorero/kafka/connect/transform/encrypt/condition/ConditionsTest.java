package com.github.rerorero.kafka.connect.transform.encrypt.condition;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionsTest {
    @Test
    void testConditionsWithMap() {
        Conditions sut = new Conditions("$.foo", "bingo");

        Map<String, Object> m = new HashMap<>();
        assertFalse(sut.mapCondition.accept(m));

        m.put("foo", "bingo");
        assertTrue(sut.mapCondition.accept(m));

        m.put("foo", "ouch");
        assertFalse(sut.mapCondition.accept(m));
    }

    @Test
    void testConditionsWithStruct() {
        Conditions sut = new Conditions("$.foo", "bingo");

        Schema schema = SchemaBuilder.struct()
                .field("foo", SchemaBuilder.string().optional())
                .build();
        Struct s = new Struct(schema);

        assertFalse(sut.structCondition.accept(s));

        s.put("foo", "bingo");
        assertTrue(sut.structCondition.accept(s));

        s.put("foo", "ouch");
        assertFalse(sut.structCondition.accept(s));
    }
}