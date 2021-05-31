package com.github.rerorero.kafka.kms;

import java.util.Objects;

public class CryptoConfig {
    protected final Item.Encoding outputEncoding;

    public CryptoConfig(Item.Encoding outputEncoding) {
        this.outputEncoding = outputEncoding;
    }

    public Item.Encoding getOutputEncoding() {
        return outputEncoding;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CryptoConfig that = (CryptoConfig) o;
        return outputEncoding == that.outputEncoding;
    }

    @Override
    public int hashCode() {
        return Objects.hash(outputEncoding);
    }
}