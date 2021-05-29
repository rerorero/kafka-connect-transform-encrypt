package com.github.rerorero.kafka.aws;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoAlgorithm;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
import com.github.rerorero.kafka.kms.Item;
import com.github.rerorero.kafka.kms.Service;

import java.util.Map;

public abstract class AWSKeyManagementService implements Service {
    protected final AwsCrypto client;
    protected final KmsMasterKeyProvider keyProvider;
    protected final AWSKMSCryptoConfig config;

    AWSKeyManagementService(AWSKMSCryptoConfig config) {
        final AwsCrypto.Builder builder = AwsCrypto.builder();
        config.getEncryptionAlgorithm().ifPresent(a -> builder.withEncryptionAlgorithm(CryptoAlgorithm.valueOf(a)));
        this.client = builder.build();
        this.keyProvider = KmsMasterKeyProvider.builder()
                .withCredentials(config.getCreds())
                .withDefaultRegion(config.getRegion())
                .buildStrict(config.getKeyID());
        this.config = config;
    }

    public static class EncryptService extends AWSKeyManagementService {
        public EncryptService(AWSKMSCryptoConfig config) {
            super(config);
        }

        @Override
        public <F> Map<F, Item> doCrypto(Map<F, Item> items) {
            return null;
        }

        Item encrypt(Item item) {
            CryptoResult<byte[], KmsMasterKey> res = client.encryptData(keyProvider, item.asBytes(), config.getContext());
            return Item.fromBytes(res.getResult(), config.getOutputEncoding());
        }
    }
}
