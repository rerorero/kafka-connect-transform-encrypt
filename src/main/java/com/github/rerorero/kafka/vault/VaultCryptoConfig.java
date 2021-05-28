package com.github.rerorero.kafka.vault;

import com.github.rerorero.kafka.kms.CryptoConfig;

import java.util.Optional;

public class VaultCryptoConfig extends CryptoConfig {
    private String keyName;
    private Optional<String> context;

    public VaultCryptoConfig(CryptoConfig common, String keyName, Optional<String> context) {
        super(common);
        this.keyName = keyName;
        this.context = context;
    }

    String getKeyName() {
        return keyName;
    }

    Optional<String> getContext() {
        return context;
    }
}
