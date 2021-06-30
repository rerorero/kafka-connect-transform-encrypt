package com.github.rerorero.kafka.gcp;

import com.amazonaws.SdkClientException;
import com.github.rerorero.kafka.connect.transform.encrypt.exception.ClientErrorException;
import com.github.rerorero.kafka.connect.transform.encrypt.exception.ServerErrorException;
import com.github.rerorero.kafka.connect.transform.encrypt.exception.ServiceException;
import com.github.rerorero.kafka.kms.Item;
import com.github.rerorero.kafka.kms.Service;
import com.github.rerorero.kafka.util.Pair;
import com.google.api.gax.rpc.ApiException;
import com.google.api.resourcenames.ResourceName;
import com.google.cloud.kms.v1.*;
import com.google.protobuf.ByteString;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public abstract class GCPKeyManagementService implements Service {
    protected KeyManagementServiceClient client;

    protected GCPKeyManagementService(GCPKMSCryptoConfig config) {
        try {
            this.client = KeyManagementServiceClient.create();
        } catch (IOException e) {
            throw new ServiceException("unable to initialize Cloud KMS client", e);
        }
    }


    @Override
    public void init() {}

    @Override
    public void close() {
        client.close();
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

    protected abstract Item callEndpoint(String field, Object item);

    public static class EncryptService extends GCPKeyManagementService {
        private final ResourceName keyName;

        public EncryptService(GCPKMSCryptoConfig config) {
            super(config);
            keyName = config.getEncryptKeyName();
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

    public static class AsymmetricEncryptService extends GCPKeyManagementService {
        private final GCPKMSCryptoConfig config;
        private java.security.PublicKey publicKey;

        public AsymmetricEncryptService(GCPKMSCryptoConfig config) {
            super(config);
            this.config = config;
        }

        @Override
        public void init() {
            try {
                final CryptoKeyVersionName keyName = config.getVersionedKeyName()
                        .orElseThrow(() -> new ClientErrorException("key version is required for asymmetric encryption"));
                final PublicKey pubKey = client.getPublicKey(keyName);
                final byte[] derKey = convertPemToDer(pubKey.getPem());
                final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(derKey);
                publicKey = config.getAsymmetricKeyFactory().generatePublic(keySpec);
            } catch (InvalidKeySpecException e) {
                throw new ClientErrorException(e);
            } catch (ApiException e) {
                throw new ServerErrorException("unable to get public key due to API error", e);
            }
        }

        // Converts a base64-encoded PEM certificate like the one returned from Cloud
        // KMS into a DER formatted certificate for use with the Java APIs.
        private static byte[] convertPemToDer(String pem) {
            BufferedReader bufferedReader = new BufferedReader(new StringReader(pem));
            String encoded =
                    bufferedReader
                            .lines()
                            .filter(line -> !line.startsWith("-----BEGIN") && !line.startsWith("-----END"))
                            .collect(Collectors.joining());
            return Base64.getDecoder().decode(encoded);
        }

        @Override
        protected Item callEndpoint(String field, Object item) {
            byte[] bytes;
            if (item instanceof String) {
                bytes = ((String) item).getBytes();
            } else if (item instanceof byte[]) {
                bytes = (byte[]) item;
            } else {
                throw new ClientErrorException("type '" + item.getClass().getTypeName() + "' for field '" + field + "' is not supported");
            }

            final Cipher cipher = config.getAsymmetricCipher(); // Cipher is not thread-safe.
            try {
                cipher.init(Cipher.ENCRYPT_MODE, publicKey, config.getOAEPSpec());
                final byte[] ciphertext = cipher.doFinal(bytes);
                return new Item.CipherBytes(ciphertext);
            } catch (InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
                throw new ClientErrorException("unable to encrypt the field:" + field + " with the public key", e);
            }
        }
    }

    public static class DecryptService extends GCPKeyManagementService {
        private final CryptoKeyName keyName;

        public DecryptService(GCPKMSCryptoConfig config) {
            super(config);
            keyName = config.getKeyName();
        }

        @Override
        protected Item callEndpoint(String field, Object item) {
            final ByteString bs = itemToByteStringForDecrypt(field, item);
            DecryptResponse response = client.decrypt(keyName, bs);
            return new Item.PlainBytes(response.getPlaintext().toByteArray());
        }
    }

    public static class AsymmetricDecryptService extends GCPKeyManagementService {
        private final CryptoKeyVersionName keyName;

        public AsymmetricDecryptService(GCPKMSCryptoConfig config) {
            super(config);
            keyName = config.getVersionedKeyName()
                    .orElseThrow(() -> new ClientErrorException("key version is required for asymmetric encryption"));
        }

        @Override
        protected Item callEndpoint(String field, Object item) {
            final ByteString bs = itemToByteStringForDecrypt(field, item);
            AsymmetricDecryptResponse response = client.asymmetricDecrypt(keyName, bs);
            return new Item.PlainBytes(response.getPlaintext().toByteArray());
        }
    }

    private static ByteString itemToByteStringForDecrypt(String field, Object item) {
        if (item instanceof String) {
            return ByteString.copyFrom(Base64.getDecoder().decode((String) item));
        } else if (item instanceof byte[]) {
            return ByteString.copyFrom((byte[]) item);
        } else {
            throw new ClientErrorException("type '" + item.getClass().getTypeName() + "' for field '" + field + "' is not supported");
        }
    }

    // visible for testing
    void setClient(KeyManagementServiceClient client) {
        this.client = client;
    }
}
