package com.github.rerorero.kafka.gcp;

import com.github.rerorero.kafka.connect.transform.encrypt.exception.ClientErrorException;
import com.google.api.resourcenames.ResourceName;
import com.google.cloud.kms.v1.CryptoKeyName;
import com.google.cloud.kms.v1.CryptoKeyVersionName;
import com.google.cloud.kms.v1.KeyManagementServiceClient;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.MGF1ParameterSpec;
import java.util.Optional;

public class GCPKMSCryptoConfig {
    private final String keyProjectID;
    private final String keyLocationID;
    private final String keyRingID;
    private final String keyID;
    private final Optional<String> keyVersionID;

    public GCPKMSCryptoConfig(
            String keyProjectID,
            String keyLocationID,
            String keyRingID,
            String keyID,
            Optional<String> keyVersionID
    ) {
        this.keyProjectID = keyProjectID;
        this.keyLocationID = keyLocationID;
        this.keyRingID = keyRingID;
        this.keyID = keyID;
        this.keyVersionID = keyVersionID;
    }

    CryptoKeyName getKeyName() {
        return CryptoKeyName.of(keyProjectID, keyLocationID, keyRingID, keyID);
    }

    ResourceName getEncryptKeyName() {
        return getVersionedKeyName().map(k -> (ResourceName) k).orElse(getKeyName());
    }

    Optional<CryptoKeyVersionName> getVersionedKeyName() {
        return keyVersionID.map(version -> CryptoKeyVersionName.of(keyProjectID, keyLocationID, keyRingID, keyID, version));
    }

    KeyFactory getAsymmetricKeyFactory() {
        try {
            return KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new ClientErrorException(e);
        }
    }

    OAEPParameterSpec getOAEPSpec() {
        return new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
    }

    Cipher getAsymmetricCipher() {
        try {
            return Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new ClientErrorException(e);
        }
    }

    KeyManagementServiceClient getKMSClient() {
        try {
            return KeyManagementServiceClient.create();
        } catch (IOException e) {
            throw new ClientErrorException("unable to create Cloud KMS client", e);
        }
    }
}
