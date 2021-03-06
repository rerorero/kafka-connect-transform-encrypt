package io.github.rerorero.kafka.kms;

import java.util.Map;

public interface Service {
    <F> Map<F, Item> doCrypto(Map<F, Object> items);

    void init();
    void close();
}
