package com.github.rerorero.kafka.aws;

import com.amazonaws.encryptionsdk.AwsCrypto;
import com.amazonaws.encryptionsdk.CryptoAlgorithm;
import com.amazonaws.encryptionsdk.CryptoResult;
import com.amazonaws.encryptionsdk.kms.KmsMasterKey;
import com.amazonaws.encryptionsdk.kms.KmsMasterKeyProvider;
import com.github.rerorero.kafka.connect.transform.encrypt.exception.ClientErrorException;
import com.github.rerorero.kafka.connect.transform.encrypt.exception.ServerErrorException;
import com.github.rerorero.kafka.kms.Item;
import com.github.rerorero.kafka.kms.Service;
import com.github.rerorero.kafka.util.Pair;

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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


    @Override
    public <F> Map<F, Item> doCrypto(Map<F, Object> items) {
        List<CompletableFuture<Pair<F, Item>>> futureList = new ArrayList<>();
        items.forEach((field, item) -> {
            futureList.add(CompletableFuture.supplyAsync(() -> {
                Item converted = callEndpoint(field.toString(), item);
                return new Pair(field, converted);
            }));
        });

        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()])).join();

        Map<F, Item> out = new HashMap<>();
        futureList.forEach(result -> {
            try {
                Pair<F, Item> pair = result.get();
                out.put(pair.key, pair.value);
            } catch (ExecutionException | InterruptedException e) {
                throw new ServerErrorException("failed to get concurrent results", e);
            }
        });

        return out;
    }

    protected abstract Item callEndpoint(String field, Object item);

    public static class EncryptService extends AWSKeyManagementService {
        public EncryptService(AWSKMSCryptoConfig config) {
            super(config);
        }

        @Override
        protected Item callEndpoint(String field, Object item) {
            byte[] parameter;
            if (item instanceof String) {
                parameter = ((String) item).getBytes(Charset.defaultCharset());
            } else if (item instanceof byte[]) {
                parameter = (byte[]) item;
            } else {
                throw new ClientErrorException("type '" + item.getClass().getTypeName() + "' for field '" + field + "' is not supported");
            }
            CryptoResult<byte[], KmsMasterKey> res = client.encryptData(keyProvider, parameter, config.getContext());
            return new Item.BytesItem(res.getResult());
        }
    }

    public static class DecryptService extends AWSKeyManagementService {
        public DecryptService(AWSKMSCryptoConfig config) {
            super(config);
        }

        @Override
        protected Item callEndpoint(String field, Object item) {
            byte[] parameter;
            if (item instanceof String) {
                parameter = Base64.getDecoder().decode((String) item);
            } else if (item instanceof byte[]) {
                parameter = (byte[]) item;
            } else {
                throw new ClientErrorException("type '" + item.getClass().getTypeName() + "' for field '" + field + "' is not supported");
            }

            CryptoResult<byte[], KmsMasterKey> res = client.decryptData(keyProvider, parameter);

            // verify decrypted key and context
            if (!res.getMasterKeyIds().get(0).equals(config.getKeyID())) {
                throw new ClientErrorException("Master key id used to decrypt is not matched: " + res.getMasterKeyIds().get(0));
            }

            if (!config.getContext().entrySet().stream().allMatch(e ->
                    e.getValue().equals(res.getEncryptionContext().get(e.getKey())))) {
                throw new ClientErrorException("Encryption context is wrong");
            }

            return new Item.BytesItem(res.getResult());
        }
    }
}
