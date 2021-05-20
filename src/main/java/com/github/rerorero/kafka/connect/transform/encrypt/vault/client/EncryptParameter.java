package com.github.rerorero.kafka.connect.transform.encrypt.vault.client;

import java.util.Objects;
import java.util.Optional;

/**
 * Plain data information to be encrypted, passed to the vault encryption api
 */
public class EncryptParameter {
    final String plainTextBase64;
    final Optional<Integer> keyVersion;
    final Optional<String> context;

    public EncryptParameter(String plainTextBase64, Optional<String> context) {
        this.plainTextBase64 = plainTextBase64;
        this.context = context;
        // bug: as of writing this, key_version doesn't work with batch input.
        // ref. https://github.com/hashicorp/vault/issues/10232
        this.keyVersion = Optional.empty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EncryptParameter that = (EncryptParameter) o;
        return Objects.equals(plainTextBase64, that.plainTextBase64) && Objects.equals(keyVersion, that.keyVersion) && Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(plainTextBase64, keyVersion, context);
    }

    @Override
    public String toString() {
        return "EncryptParameter{" +
                "plainTextBase64='" + plainTextBase64 + '\'' +
                ", keyVersion=" + keyVersion +
                ", context=" + context +
                '}';
    }
}
