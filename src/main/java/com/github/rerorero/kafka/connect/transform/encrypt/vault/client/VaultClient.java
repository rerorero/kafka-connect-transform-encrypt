package com.github.rerorero.kafka.connect.transform.encrypt.vault.client;

import java.util.List;

public interface VaultClient {
    /**
     * Encrypt multiple plain items with the given key.
     *
     * @param keyName The Vault key name
     * @param items The plain item information to be encrypted
     * @return {@link List} of cipher text.
     */
    List<String> encrypt(String keyName, List<EncryptParameter> items);

    /**
     * Decrypt multiple cipher text with the given key.
     *
     * @param keyName The Vault key name
     * @param items The cipher item information to be decrypted
     * @return {@link List} of plain item.
     */
    List<String> decrypt(String keyName, List<DecryptParameter> items);
}
