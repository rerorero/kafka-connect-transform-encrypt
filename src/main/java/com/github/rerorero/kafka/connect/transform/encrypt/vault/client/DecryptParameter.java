package com.github.rerorero.kafka.connect.transform.encrypt.vault.client;

import java.util.Objects;
import java.util.Optional;

/**
 * Cipher item information to be decrypted, passed to the vault decryption api
 */
public class DecryptParameter {
    final String cipherText;
    final Optional<String> context;

    public DecryptParameter(String cipherText, Optional<String> context) {
        this.cipherText = cipherText;
        this.context = context;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DecryptParameter that = (DecryptParameter) o;
        return Objects.equals(cipherText, that.cipherText) && Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cipherText, context);
    }

    @Override
    public String toString() {
        return "DecryptParameter{" +
                "cipherText='" + cipherText + '\'' +
                ", context=" + context +
                '}';
    }
}
