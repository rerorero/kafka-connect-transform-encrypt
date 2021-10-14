package io.github.rerorero.kafka.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class AWSKMSCryptoConfig {
    private final Optional<AWSCredentials> creds;
    private final Optional<String> region;
    private final String keyID;
    private final Map<String, String> context;
    private final Optional<String> encryptionAlgorithm;
    private final Optional<String> kmsEndpoint;

    public AWSKMSCryptoConfig(
            Optional<AWSCredentials> creds,
            Optional<String> region,
            String keyID,
            Map<String, String> context,
            Optional<String> encryptionAlgorithm,
            Optional<String> kmsEndpoint
    ) {
        this.creds = creds;
        this.region = region;
        this.keyID = keyID;
        this.context = context != null ? context : new HashMap<>();
        this.encryptionAlgorithm = encryptionAlgorithm;
        this.kmsEndpoint = kmsEndpoint;
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

    AWSCredentialsProvider getCredentialProvider() {
        return creds.<AWSCredentialsProvider>map(c -> new AWSStaticCredentialsProvider(c))
                .orElse(DefaultAWSCredentialsProviderChain.getInstance());
    }

    Optional<String> getRegion() {
        return region;
    }

    Optional<String> getKmsEndpoint() {
        return kmsEndpoint;
    }
}
