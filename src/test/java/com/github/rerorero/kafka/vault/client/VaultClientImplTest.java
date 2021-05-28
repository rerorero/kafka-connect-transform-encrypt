package com.github.rerorero.kafka.vault.client;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.github.rerorero.kafka.connect.transform.encrypt.exception.ClientErrorException;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.vault.VaultContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class VaultClientImplTest {
    private static final String VAULT_TOKEN = "dev";
    private static final String NORMAL_KEY = "mykey";
    private static final String CONVERGENT_ENCRYPT_KEY = "key2";

    public static VaultContainer container = new VaultContainer<>("vault:latest")
            .withVaultToken(VAULT_TOKEN)
            .waitingFor(Wait.forHttp("/v1/secret/test").forStatusCode(400))
            .withInitCommand(
                    String.format("secrets enable transit"),
                    String.format("write -f transit/keys/%s", NORMAL_KEY),
                    String.format("write -f transit/keys/%s convergent_encryption=true derived=true", CONVERGENT_ENCRYPT_KEY)
            );

    @BeforeAll
    public static void tearUp() {
        container.start();
    }

    @AfterAll
    public static void tearDown() {
        container.stop();
    }

    private VaultClientImpl newSUT() {
        VaultClientImpl sut = null;
        try {
            VaultConfig config = new VaultConfig()
                    .address(String.format("http://%s:%d", container.getHost(), container.getFirstMappedPort()))
                    .token(VAULT_TOKEN)
                    .build();
            sut = new VaultClientImpl(new Vault(config, 1));
        } catch (VaultException e) {
            fail(e);
        }
        return sut;
    }

    @Test
    public void testEncryptString() throws InterruptedException, IOException {
        VaultClientImpl sut = newSUT();
        String[] expected = {"RnJhbnR6", "RGllIFZlcndhbmRsdW5n"};

        // encrypt
        final List<EncryptParameter> params = Arrays.stream(expected)
                .map(e -> new EncryptParameter(e, Optional.empty()))
                .collect(Collectors.toList());

        List<String> encrypted = sut.encrypt(NORMAL_KEY, params);
        assertEquals(expected.length, encrypted.size());

        // decrypt
        final List<DecryptParameter> decItems = encrypted.stream()
                .map((String e) -> new DecryptParameter(e, Optional.empty()))
                .collect(Collectors.toList());

        List<String> decrypted = sut.decrypt(NORMAL_KEY, decItems);
        assertArrayEquals(expected, decrypted.toArray(new String[0]));
    }

    @Test
    public void testEncryptWithContext() throws InterruptedException, IOException {
        VaultClientImpl sut = newSUT();
        String[] expected = {"RnJhbnR6", "RGllIFZlcndhbmRsdW5n"};
        String[] contexts = {"Y29udGV4dDE=", "Y29udGV4dDI="};

        // encrypt
        final List<EncryptParameter> encItems = new ArrayList<>();
        for (int i = 0; i < expected.length; i++) {
            encItems.add(new EncryptParameter(expected[i], Optional.of(contexts[i])));
        }

        List<String> encrypted = sut.encrypt(NORMAL_KEY, encItems);
        assertEquals(expected.length, encrypted.size());

        // decrypt
        final List<DecryptParameter> decItems = new ArrayList<>();
        for (int i = 0; i < expected.length; i++) {
            decItems.add(new DecryptParameter(encrypted.get(i), Optional.of(contexts[i])));
        }

        List<String> decrypted = sut.decrypt(NORMAL_KEY, decItems);
        assertArrayEquals(expected, decrypted.toArray());
    }

    @Test
    public void testEncryptEmptyArgument() throws InterruptedException, IOException {
        VaultClientImpl sut = newSUT();
        List<String> encrypted = sut.encrypt(NORMAL_KEY, new ArrayList<>());
        assertEquals(0, encrypted.size());
    }

    @Test
    public void testPartiallyFailEncrypt() throws InterruptedException, IOException {
        VaultClientImpl sut = newSUT();
        String[] texts = {"RnJhbnR6", "not a base 64 string"};

        final List<EncryptParameter> params = Arrays.stream(texts)
                .map(e -> new EncryptParameter(e, Optional.empty()))
                .collect(Collectors.toList());

        assertThrows(ClientErrorException.class, () -> sut.encrypt(NORMAL_KEY, params));
    }

    @Test
    public void testPartiallyFailDecrypt() throws InterruptedException, IOException {
        VaultClientImpl sut = newSUT();
        String[] texts = {"RnJhbnR6", "RGllIFZlcndhbmRsdW5n"};

        // encrypt
        final List<EncryptParameter> encItems = new ArrayList<>();
        for (int i = 0; i < texts.length; i++) {
            encItems.add(new EncryptParameter(texts[i], Optional.empty()));
        }

        List<String> encrypted = sut.encrypt(NORMAL_KEY, encItems);
        encrypted.set(1, "invalid");

        // decrypt
        final List<DecryptParameter> decItems = new ArrayList<>();
        for (int i = 0; i < texts.length; i++) {
            decItems.add(new DecryptParameter(encrypted.get(i), Optional.empty()));
        }

        assertThrows(ClientErrorException.class, () -> sut.decrypt(NORMAL_KEY, decItems));
    }

    @Test
    public void testFailIllegalArgument() throws InterruptedException, IOException {
        VaultClientImpl sut = newSUT();
        String[] texts = {"RnJhbnR6", "RGllIFZlcndhbmRsdW5n"};
        String[] contexts = {"Y29udGV4dDE=", ""}; // context should be set either in all the request blocks or in none

        // encrypt
        final List<EncryptParameter> encItems = new ArrayList<>();
        for (int i = 0; i < texts.length; i++) {
            encItems.add(new EncryptParameter(texts[i], Optional.of(contexts[i])));
        }

        assertThrows(ClientErrorException.class, () -> sut.encrypt(NORMAL_KEY, encItems));
    }
}