package com.github.rerorero.kafka.connect.transform.encrypt.kms;

import java.util.Map;

public interface Service {
    <F> Map<F, Item> doCrypto(Map<F, Item> items);
}
