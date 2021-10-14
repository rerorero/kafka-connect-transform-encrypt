package io.github.rerorero.kafka.vault;

import java.util.Optional;

public class VaultCryptoConfig {
    private String keyName;
    private Optional<String> context;

    public VaultCryptoConfig(String keyName, Optional<String> context) {
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
