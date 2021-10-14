package io.github.rerorero.kafka.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.bettercloud.vault.json.Json;
import io.github.rerorero.kafka.connect.transform.encrypt.exception.ClientErrorException;
import io.github.rerorero.kafka.connect.transform.encrypt.exception.ServerErrorException;
import io.github.rerorero.kafka.kms.Item;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AWSKeyManagementServiceTest {
    private static GenericContainer container = new GenericContainer(DockerImageName.parse("nsmithuk/local-kms:3.9.1"))
            .waitingFor(Wait.forHttp("/").forStatusCode(405))
            .withExposedPorts(8080);

    private static String keyArn;
    private static String kmsAddr;
    private static AWSCredentials creds;

    @BeforeAll
    public static void tearUp() {
        container.start();

        try {
            // Create a key
            final ExecResult res = container.execInContainer("/bin/sh", "-c",
                    "wget -nv -O - --header='X-Amz-Target: TrentService.CreateKey' " + "" +
                            "--header='Content-Type:  application/x-amz-json-1.1' --post-data='' http://localhost:8080");
            assertEquals(0, res.getExitCode());

            keyArn = Json.parse(res.getStdout()).asObject().get("KeyMetadata").asObject().getString("Arn");

        } catch (IOException | InterruptedException e) {
            fail(e);
        }

        kmsAddr = String.format("http://%s:%d/", container.getHost(), container.getFirstMappedPort());
        creds = new BasicAWSCredentials("dummy", "dummy");
    }

    @AfterAll
    public static void tearDown() {
        container.stop();
    }

    @Test
    void testEncryptStringWithContext() {
        AWSKMSCryptoConfig config = new AWSKMSCryptoConfig(
                Optional.of(creds),
                Optional.of("us-west-1"),
                keyArn,
                Collections.singletonMap("dummy", "context"),
                Optional.of("ALG_AES_256_GCM_HKDF_SHA512_COMMIT_KEY"),
                Optional.of(kmsAddr)
        );
        AWSKeyManagementService.EncryptService encryptor = new AWSKeyManagementService.EncryptService(config);
        AWSKeyManagementService.DecryptService decryptor = new AWSKeyManagementService.DecryptService(config);

        Map<String, Object> encParams = new HashMap<>();
        encParams.put("item1", "Kafka");
        encParams.put("item2", "Frantz");

        Map<String, Item> encrypted = encryptor.doCrypto(encParams);

        Map<String, Object> decParams = new HashMap<>();
        decParams.put("item1", encrypted.get("item1").asObject(Item.Encoding.STRING));
        decParams.put("item2", encrypted.get("item2").asObject(Item.Encoding.STRING));
        Map<String, Item> actual = decryptor.doCrypto(decParams);

        Map<String, Item> expected = new HashMap<>();
        expected.put("item1", new Item.PlainBytes("Kafka".getBytes()));
        expected.put("item2", new Item.PlainBytes("Frantz".getBytes()));

        assertEquals(expected, actual);
    }

    @Test
    void testEncryptBinaryWithoutContext() {
        AWSKMSCryptoConfig config = new AWSKMSCryptoConfig(
                Optional.of(creds),
                Optional.of("us-west-1"),
                keyArn,
                null,
                Optional.empty(),
                Optional.of(kmsAddr)
        );
        AWSKeyManagementService.EncryptService encryptor = new AWSKeyManagementService.EncryptService(config);
        AWSKeyManagementService.DecryptService decryptor = new AWSKeyManagementService.DecryptService(config);

        Map<String, Object> encParams = new HashMap<>();
        encParams.put("item1", "Kafka".getBytes(Charset.defaultCharset()));
        encParams.put("item2", "Frantz".getBytes(Charset.defaultCharset()));

        Map<String, Item> encrypted = encryptor.doCrypto(encParams);

        Map<String, Object> decParams = new HashMap<>();
        decParams.put("item1", encrypted.get("item1").asObject(Item.Encoding.STRING));
        decParams.put("item2", encrypted.get("item2").asObject(Item.Encoding.STRING));
        Map<String, Item> actual = decryptor.doCrypto(decParams);

        Map<String, Item> expected = new HashMap<>();
        expected.put("item1", new Item.PlainBytes("Kafka".getBytes()));
        expected.put("item2", new Item.PlainBytes("Frantz".getBytes()));

        assertEquals(expected, actual);
    }

    @Test
    void testFailToEncryptDueToWrongEndpoint() {
        AWSKMSCryptoConfig config = new AWSKMSCryptoConfig(
                Optional.of(creds),
                Optional.of("us-west-1"),
                keyArn,
                null,
                Optional.empty(),
                Optional.of("http://unknownunknownunknown.comcom:9999/")
        );
        AWSKeyManagementService.EncryptService encryptor = new AWSKeyManagementService.EncryptService(config);
        assertThrows(ServerErrorException.class, () -> encryptor.doCrypto(Collections.singletonMap("item1", "Kafka")));
    }

    @Test
    void testFailToDecryptDueToWrongContext() {
        AWSKMSCryptoConfig config1 = new AWSKMSCryptoConfig(
                Optional.of(creds),
                Optional.of("us-west-1"),
                keyArn,
                Collections.singletonMap("dummy", "context"),
                Optional.empty(),
                Optional.of(kmsAddr)
        );
        AWSKMSCryptoConfig config2 = new AWSKMSCryptoConfig(
                Optional.of(creds),
                Optional.of("us-west-1"),
                keyArn,
                Collections.singletonMap("dummy", "wrong"),
                Optional.empty(),
                Optional.of(kmsAddr)
        );
        AWSKeyManagementService.EncryptService encryptor = new AWSKeyManagementService.EncryptService(config1);
        AWSKeyManagementService.DecryptService decryptor = new AWSKeyManagementService.DecryptService(config2);

        Map<String, Item> encrypted = encryptor.doCrypto(Collections.<String, Object>singletonMap("item1", "Kafka"));
        assertThrows(ClientErrorException.class, () -> decryptor.doCrypto(Collections.singletonMap("item1", encrypted.get("item1"))));
    }
}