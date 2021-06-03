package com.github.rerorero.kafka.gcp;

import com.google.cloud.kms.v1.CryptoKeyName;

public class GCPKMSCryptoConfig {
    private final CryptoKeyName keyName;

    public GCPKMSCryptoConfig(
            String keyProjectID,
            String keyLocationID,
            String keyRingID,
            String keyID
    ) {
        this.keyName = CryptoKeyName.of(keyProjectID, keyLocationID, keyRingID, keyID);
    }

    CryptoKeyName getKeyName() {
        return keyName;
    }
}
