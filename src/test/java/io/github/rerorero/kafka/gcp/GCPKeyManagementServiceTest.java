package io.github.rerorero.kafka.gcp;

import io.github.rerorero.kafka.connect.transform.encrypt.exception.ClientErrorException;
import io.github.rerorero.kafka.connect.transform.encrypt.exception.ServerErrorException;
import io.github.rerorero.kafka.kms.Item;
import com.google.api.gax.grpc.GrpcStatusCode;
import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.kms.v1.PublicKey;
import com.google.cloud.kms.v1.*;
import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;
import io.grpc.Status;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GCPKeyManagementServiceTest {
    private static KeyPair rsaPair;

    private static class TestConfig extends GCPKMSCryptoConfig {
        private final KeyManagementServiceClient client;

        TestConfig(Optional<String> keyVersion, KeyManagementServiceClient client) {
            super("project", "us-east1", "keyring", "my-key", keyVersion);
            this.client = client;
        }

        @Override
        KeyManagementServiceClient getKMSClient() {
            return client;
        }
    }

    @BeforeAll
    public static void tearUp() throws NoSuchAlgorithmException {
        KeyPairGenerator fact = KeyPairGenerator.getInstance("RSA");
        fact.initialize(3096);
        rsaPair = fact.generateKeyPair();
    }

    @Test
    void testSymmetricEncrypt() {
        KeyManagementServiceClient cli = mock(KeyManagementServiceClient.class);
        GCPKMSCryptoConfig config = new TestConfig(Optional.of("123"), cli);
        GCPKeyManagementService.EncryptService sut = new GCPKeyManagementService.EncryptService(config);
        sut.init();

        when(cli.encrypt(config.getEncryptKeyName(), ByteString.copyFromUtf8("Kafka")))
                .thenReturn(EncryptResponse.newBuilder().setCiphertext(ByteString.copyFromUtf8("akfaK")).build());
        when(cli.encrypt(config.getEncryptKeyName(), ByteString.copyFromUtf8("Frantz")))
                .thenReturn(EncryptResponse.newBuilder().setCiphertext(ByteString.copyFromUtf8("ztnarF")).build());

        Map<String, Object> params = new HashMap<>();
        params.put("item1", "Kafka");
        params.put("item2", "Frantz");

        Map<String, Item> actual = sut.doCrypto(params);

        Map<String, Item> expected = new HashMap<>();
        expected.put("item1", new Item.CipherBytes("akfaK".getBytes()));
        expected.put("item2", new Item.CipherBytes("ztnarF".getBytes()));

        assertEquals(expected, actual);
    }

    @Test
    void testSymmetricDecrypt() {
        KeyManagementServiceClient cli = mock(KeyManagementServiceClient.class);
        GCPKMSCryptoConfig config = new TestConfig(Optional.empty(), cli);
        GCPKeyManagementService.DecryptService sut = new GCPKeyManagementService.DecryptService(config);
        sut.init();

        when(cli.decrypt(config.getKeyName(), ByteString.copyFromUtf8("Kafka")))
                .thenReturn(DecryptResponse.newBuilder().setPlaintext(ByteString.copyFromUtf8("akfaK")).build());
        when(cli.decrypt(config.getKeyName(), ByteString.copyFromUtf8("Frantz")))
                .thenReturn(DecryptResponse.newBuilder().setPlaintext(ByteString.copyFromUtf8("ztnarF")).build());

        Map<String, Object> params = new HashMap<>();
        params.put("item1", "Kafka".getBytes());
        params.put("item2", "Frantz".getBytes());

        Map<String, Item> actual = sut.doCrypto(params);

        Map<String, Item> expected = new HashMap<>();
        expected.put("item1", new Item.PlainBytes("akfaK".getBytes()));
        expected.put("item2", new Item.PlainBytes("ztnarF".getBytes()));

        assertEquals(expected, actual);
    }

    @Test
    void testAsymmetricEncrypt() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        KeyManagementServiceClient cli = mock(KeyManagementServiceClient.class);
        GCPKMSCryptoConfig config = new TestConfig(Optional.of("123"), cli);
        GCPKeyManagementService.AsymmetricEncryptService sut = new GCPKeyManagementService.AsymmetricEncryptService(config);

        when(cli.getPublicKey(CryptoKeyVersionName.of("project", "us-east1", "keyring", "my-key", "123")))
                .thenReturn(generatePublicKeyCert());
        sut.init();

        Map<String, Object> params = new HashMap<>();
        params.put("item1", "Kafka");
        params.put("item2", "Frantz");

        Map<String, Item> actual = sut.doCrypto(params);

        // decrypt with private key.
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        KeyFactory factory = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = factory.generatePrivate(factory.getKeySpec(rsaPair.getPrivate(), RSAPrivateKeySpec.class));
        cipher.init(Cipher.DECRYPT_MODE, privateKey, config.getOAEPSpec());

        assertArrayEquals(cipher.doFinal((byte[]) actual.get("item1").asObject(Item.Encoding.BINARY)), "Kafka".getBytes());
        assertArrayEquals(cipher.doFinal((byte[]) actual.get("item2").asObject(Item.Encoding.BINARY)), "Frantz".getBytes());
    }

    @Test
    void testAsymmetricDecrypt() {
        KeyManagementServiceClient cli = mock(KeyManagementServiceClient.class);
        GCPKMSCryptoConfig config = new TestConfig(Optional.of("123"), cli);
        GCPKeyManagementService.AsymmetricDecryptService sut = new GCPKeyManagementService.AsymmetricDecryptService(config);
        sut.init();

        when(cli.asymmetricDecrypt(config.getVersionedKeyName().get(), ByteString.copyFromUtf8("Kafka")))
                .thenReturn(AsymmetricDecryptResponse.newBuilder().setPlaintext(ByteString.copyFromUtf8("akfaK")).build());
        when(cli.asymmetricDecrypt(config.getVersionedKeyName().get(), ByteString.copyFromUtf8("Frantz")))
                .thenReturn(AsymmetricDecryptResponse.newBuilder().setPlaintext(ByteString.copyFromUtf8("ztnarF")).build());

        Map<String, Object> params = new HashMap<>();
        params.put("item1", "Kafka".getBytes());
        params.put("item2", "Frantz".getBytes());

        Map<String, Item> actual = sut.doCrypto(params);

        Map<String, Item> expected = new HashMap<>();
        expected.put("item1", new Item.PlainBytes("akfaK".getBytes()));
        expected.put("item2", new Item.PlainBytes("ztnarF".getBytes()));

        assertEquals(expected, actual);
    }

    private static PublicKey generatePublicKeyCert() {
        // Format the public key into a PEM encoded Certificate.
        final String cert = "-----BEGIN RSA PUBLIC KEY-----\n"
                + BaseEncoding.base64().withSeparator("\n", 64).encode(rsaPair.getPublic().getEncoded())
                + "\n"
                + "-----END RSA PUBLIC KEY-----\n";
        return PublicKey.newBuilder().setPem(cert).build();
    }

    @Test
    void testUnableToInitAsymmetricEncryptService() {
        // key version is missing
        GCPKMSCryptoConfig config = new TestConfig(Optional.empty(), null);
        GCPKeyManagementService.AsymmetricEncryptService sut = new GCPKeyManagementService.AsymmetricEncryptService(config);
        assertThrows(ClientErrorException.class, () -> sut.init());
    }

    @Test
    void testUnableToGetPublicKey() {
        KeyManagementServiceClient cli = mock(KeyManagementServiceClient.class);
        GCPKMSCryptoConfig config = new TestConfig(Optional.of("123"), cli);
        GCPKeyManagementService.AsymmetricEncryptService sut = new GCPKeyManagementService.AsymmetricEncryptService(config);

        when(cli.getPublicKey(CryptoKeyVersionName.of("project", "us-east1", "keyring", "my-key", "123")))
                .thenThrow(new NotFoundException(new RuntimeException(), GrpcStatusCode.of(Status.Code.NOT_FOUND), false));
        assertThrows(ServerErrorException.class, () -> sut.init());
    }
}