package com.github.rerorero.kafka.vault;

import com.github.rerorero.kafka.connect.transform.encrypt.exception.ServiceException;
import com.github.rerorero.kafka.kms.Item;
import com.github.rerorero.kafka.vault.client.DecryptParameter;
import com.github.rerorero.kafka.vault.client.EncryptParameter;
import com.github.rerorero.kafka.vault.client.VaultClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class VaultServiceTest {
    private static String keyName = "key";

    private static Stream<Arguments> encryptArguments() {
        // case1 string -> string: with context
        VaultCryptoConfig config1 = new VaultCryptoConfig(keyName, Optional.of("context"));
        Map<Integer, Item> param1 = new HashMap<>();
        param1.put(1, new Item.StringItem("Frantz"));
        param1.put(2, new Item.StringItem("Kafka"));
        List<String> mockedResult1 = new ArrayList<>();
        mockedResult1.add("encrypted-Frantz");
        mockedResult1.add("encrypted-Kafka");
        List<EncryptParameter> expectedMockArgs1 = new ArrayList<>();
        expectedMockArgs1.add(new EncryptParameter("RnJhbnR6", Optional.of("context")));
        expectedMockArgs1.add(new EncryptParameter("S2Fma2E=", Optional.of("context")));
        Map<Integer, Item> expected1 = new HashMap<>();
        expected1.put(1, new Item.StringItem("encrypted-Frantz"));
        expected1.put(2, new Item.StringItem("encrypted-Kafka"));

        // case2 binary -> string: without context
        VaultCryptoConfig config2 = new VaultCryptoConfig(keyName, Optional.empty());
        Map<Integer, Item> param2 = new HashMap<>();
        param2.put(1, new Item.BytesItem(new byte[]{70, 114, 97, 110, 116, 122}));
        param2.put(2, new Item.BytesItem(new byte[]{75, 97, 102, 107, 97}));
        List<String> mockedResult2 = new ArrayList<>();
        mockedResult2.add("encrypted-Frantz");
        mockedResult2.add("encrypted-Kafka");
        List<EncryptParameter> expectedMockArgs2 = new ArrayList<>();
        expectedMockArgs2.add(new EncryptParameter("RnJhbnR6", Optional.empty()));
        expectedMockArgs2.add(new EncryptParameter("S2Fma2E=", Optional.empty()));
        Map<Integer, Item> expected2 = new HashMap<>();
        expected2.put(1, new Item.StringItem("encrypted-Frantz"));
        expected2.put(2, new Item.StringItem("encrypted-Kafka"));

        // case3 empty
        VaultCryptoConfig config3 = new VaultCryptoConfig(keyName, Optional.of("context"));
        Map<Integer, Item> param3 = new HashMap<>();
        List<String> mockedResult3 = new ArrayList<>();
        List<EncryptParameter> expectedMockArgs3 = new ArrayList<>();
        Map<Integer, Item> expected3 = new HashMap<>();

        return Stream.of(
                Arguments.of(config1, param1, mockedResult1, expectedMockArgs1, expected1),
                Arguments.of(config2, param2, mockedResult2, expectedMockArgs2, expected2),
                Arguments.of(config3, param3, mockedResult3, expectedMockArgs3, expected3)
        );
    }

    private static Stream<Arguments> decryptArguments() {
        // case1 string -> string: with context
        VaultCryptoConfig config1 = new VaultCryptoConfig(keyName, Optional.of("context"));
        Map<Integer, Item> param1 = new HashMap<>();
        param1.put(1, new Item.StringItem("encrypted-Frantz"));
        param1.put(2, new Item.StringItem("encrypted-Kafka"));
        List<String> mockedResult1 = new ArrayList<>();
        mockedResult1.add("RnJhbnR6");
        mockedResult1.add("S2Fma2E=");
        List<DecryptParameter> expectedMockArgs1 = new ArrayList<>();
        expectedMockArgs1.add(new DecryptParameter("encrypted-Frantz", Optional.of("context")));
        expectedMockArgs1.add(new DecryptParameter("encrypted-Kafka", Optional.of("context")));
        Map<Integer, Item> expected1 = new HashMap<>();
        expected1.put(1, new Item.BytesItem("Frantz".getBytes(Charset.defaultCharset())));
        expected1.put(2, new Item.BytesItem("Kafka".getBytes(Charset.defaultCharset())));

        // case2 binary -> string: without context
        VaultCryptoConfig config2 = new VaultCryptoConfig(keyName, Optional.empty());
        Map<Integer, Item> param2 = new HashMap<>();
        param2.put(1, new Item.BytesItem(new byte[]{111, 110, 101})); // "one"
        param2.put(2, new Item.BytesItem(new byte[]{116, 119, 111})); // "two"
        List<String> mockedResult2 = new ArrayList<>();
        mockedResult2.add("RnJhbnR6");
        mockedResult2.add("S2Fma2E=");
        List<DecryptParameter> expectedMockArgs2 = new ArrayList<>();
        expectedMockArgs2.add(new DecryptParameter("one", Optional.empty()));
        expectedMockArgs2.add(new DecryptParameter("two", Optional.empty()));
        Map<Integer, Item> expected2 = new HashMap<>();
        expected2.put(1, new Item.BytesItem("Frantz".getBytes(Charset.defaultCharset())));
        expected2.put(2, new Item.BytesItem("Kafka".getBytes(Charset.defaultCharset())));

        // case5 empty
        VaultCryptoConfig config3 = new VaultCryptoConfig(keyName, Optional.of("context"));
        Map<Integer, Item> param3 = new HashMap<>();
        List<String> mockedResult3 = new ArrayList<>();
        List<DecryptParameter> expectedMockArgs3 = new ArrayList<>();
        Map<Integer, Item> expected3 = new HashMap<>();

        return Stream.of(
                Arguments.of(config1, param1, mockedResult1, expectedMockArgs1, expected1),
                Arguments.of(config2, param2, mockedResult2, expectedMockArgs2, expected2),
                Arguments.of(config3, param3, mockedResult3, expectedMockArgs3, expected3)
        );
    }

    @ParameterizedTest
    @MethodSource("encryptArguments")
    public void testEncryptService(
            VaultCryptoConfig conf,
            Map<Integer, Item> param,
            List<String> mockResult,
            List<EncryptParameter> expectedMockArgs,
            Map<Integer, Item> expected
    ) {
        VaultClient vault = mock(VaultClient.class);
        ArgumentCaptor<List<EncryptParameter>> paramCaptor = ArgumentCaptor.forClass(List.class);
        when(vault.encrypt(any(), any())).thenReturn(mockResult);

        VaultService sut = new VaultService.EncryptService(vault, conf);
        Map<Integer, Item> actual = sut.doCrypto(param);

        assertEquals(expected, actual);

        verify(vault).encrypt(eq(keyName), paramCaptor.capture());
        assertEquals(expectedMockArgs, paramCaptor.getValue());
    }

    @ParameterizedTest
    @MethodSource("decryptArguments")
    public void testDecryptService(
            VaultCryptoConfig conf,
            Map<Integer, Item> param,
            List<String> mockResult,
            List<DecryptParameter> expectedMockArgs,
            Map<Integer, Item> expected
    ) {
        VaultClient vault = mock(VaultClient.class);
        ArgumentCaptor<List<DecryptParameter>> paramCaptor = ArgumentCaptor.forClass(List.class);
        when(vault.decrypt(any(), any())).thenReturn(mockResult);

        VaultService sut = new VaultService.DecryptService(vault, conf);
        Map<Integer, Item> actual = sut.doCrypto(param);

        assertEquals(expected, actual);

        verify(vault).decrypt(eq(keyName), paramCaptor.capture());
        assertEquals(expectedMockArgs, paramCaptor.getValue());
    }

    @Test
    public void testEncryptFailure() {
        VaultClient vault = mock(VaultClient.class);
        when(vault.encrypt(any(), any())).thenThrow(new ServiceException("failed"));

        VaultCryptoConfig conf = new VaultCryptoConfig(keyName, Optional.empty());
        VaultService sut = new VaultService.EncryptService(vault, conf);

        assertThrows(ServiceException.class, () -> sut.doCrypto(new HashMap()));
    }

    @Test
    public void testDecryptFailure() {
        VaultClient vault = mock(VaultClient.class);
        when(vault.decrypt(any(), any())).thenThrow(new ServiceException("failed"));

        VaultCryptoConfig conf = new VaultCryptoConfig(keyName, Optional.empty());
        VaultService sut = new VaultService.DecryptService(vault, conf);

        assertThrows(ServiceException.class, () -> sut.doCrypto(new HashMap()));
    }
}