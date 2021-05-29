package com.github.rerorero.kafka.aws;

import com.amazonaws.auth.AWSCredentials;
import com.github.rerorero.kafka.kms.CryptoConfig;

import java.util.Map;
import java.util.Optional;

public class AWSKMSCryptoConfig extends CryptoConfig {
    private final AWSCredentials creds;
    private final String region;
    private final String keyID;
    private final Map<String, String> context;
    private final Optional<String> encryptionAlgorithm;

    public AWSKMSCryptoConfig(
            CryptoConfig common,
            AWSCredentials creds,
            String region,
            String keyID,
            Map<String, String> context,
            Optional<String> encryptionAlgorithm
    ) {
        super(common);
        this.creds = creds;
        this.region = region;
        this.keyID = keyID;
        this.context = context;
        this.encryptionAlgorithm = encryptionAlgorithm;
    }

    String getKeyID() {
        return keyID;
    }

    Map<String, String> getContext() {
        return context;
    }

    Optional<String> getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    AWSCredentials getCreds() {
        return creds;
    }

    String getRegion() {
        return region;
    }
}
