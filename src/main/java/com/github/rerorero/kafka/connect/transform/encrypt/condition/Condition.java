package com.github.rerorero.kafka.connect.transform.encrypt.condition;

public interface Condition<R> {
    boolean accept(R record);
}

