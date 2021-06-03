package com.github.rerorero.kafka.gcp;

import com.amazonaws.SdkClientException;
import com.github.rerorero.kafka.connect.transform.encrypt.exception.ClientErrorException;
import com.github.rerorero.kafka.connect.transform.encrypt.exception.ServerErrorException;
import com.github.rerorero.kafka.connect.transform.encrypt.exception.ServiceException;
import com.github.rerorero.kafka.kms.Item;
import com.github.rerorero.kafka.kms.Service;
import com.github.rerorero.kafka.util.Pair;
import com.google.cloud.kms.v1.CryptoKeyName;
import com.google.cloud.kms.v1.DecryptResponse;
import com.google.cloud.kms.v1.EncryptResponse;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public abstract class GCPKeyManagementService implements Service {
    protected final KeyManagementServiceClient client;
    protected final CryptoKeyName keyName;

    protected GCPKeyManagementService(GCPKMSCryptoConfig config) {
        try {
            this.client = KeyManagementServiceClient.create();
        } catch (IOException e) {
            throw new ServiceException("unable to initialize Cloud KMS client", e);
        }
        this.keyName = config.getKeyName();
    }


    @Override
    public <F> Map<F, Item> doCrypto(Map<F, Object> items) {
        final List<CompletableFuture<Pair<F, Item>>> futureList = new ArrayList<>();
        items.forEach((field, item) -> {
            futureList.add(CompletableFuture.supplyAsync(() -> {
                try {
                    Item converted = callEndpoint(field.toString(), item);
                    return new Pair(field, converted);
                } catch (SdkClientException e) {
                    throw new ServerErrorException(e);
                }
            }));
        });

        try {
            CompletableFuture.allOf(futureList.toArray(new CompletableFuture[futureList.size()])).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof ServiceException) {
                throw (ServiceException) e.getCause();
            } else {
                throw new ServiceException(e);
            }
        }

        final Map<F, Item> out = new HashMap<>();
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

    @Override
    public void close() {
        client.close();
    }

    protected abstract Item callEndpoint(String field, Object item);

    public static class EncryptService extends GCPKeyManagementService {
        public EncryptService(GCPKMSCryptoConfig config) {
            super(config);
        }

        @Override
        protected Item callEndpoint(String field, Object item) {
            ByteString bs;
            if (item instanceof String) {
                bs = ByteString.copyFromUtf8((String) item);
            } else if (item instanceof byte[]) {
                bs = ByteString.copyFrom((byte[]) item);
            } else {
                throw new ClientErrorException("type '" + item.getClass().getTypeName() + "' for field '" + field + "' is not supported");
            }

            EncryptResponse response = client.encrypt(keyName, bs);
            return new Item.CipherBytes(response.getCiphertext().toByteArray());
        }
    }

    public static class DecryptService extends GCPKeyManagementService {
        public DecryptService(GCPKMSCryptoConfig config) {
            super(config);
        }

        @Override
        protected Item callEndpoint(String field, Object item) {
            ByteString bs;
            if (item instanceof String) {
                bs = ByteString.copyFrom(Base64.getDecoder().decode((String) item));
            } else if (item instanceof byte[]) {
                bs = ByteString.copyFrom((byte[]) item);
            } else {
                throw new ClientErrorException("type '" + item.getClass().getTypeName() + "' for field '" + field + "' is not supported");
            }
            DecryptResponse response = client.decrypt(keyName, bs);
            return new Item.PlainBytes(response.getPlaintext().toByteArray());
        }
    }
}
