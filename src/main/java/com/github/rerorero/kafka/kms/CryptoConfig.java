package com.github.rerorero.kafka.kms;

import java.util.Objects;

public class CryptoConfig {
    protected final Item.Encoding inputEncoding;
    protected final Item.Encoding outputEncoding;

    public CryptoConfig(Item.Encoding inputEncoding, Item.Encoding outputEncoding) {
        this.inputEncoding = inputEncoding;
        this.outputEncoding = outputEncoding;
    }

    public CryptoConfig(CryptoConfig config) {
        this(config.inputEncoding, config.outputEncoding);
    }

    public Item.Encoding getInputEncoding() {
        return inputEncoding;
    }

    public Item.Encoding getOutputEncoding() {
        return outputEncoding;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CryptoConfig that = (CryptoConfig) o;
        return inputEncoding == that.inputEncoding && outputEncoding == that.outputEncoding;
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputEncoding, outputEncoding);
    }
}