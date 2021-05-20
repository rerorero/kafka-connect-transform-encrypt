package com.github.rerorero.kafka.connect.transform.encrypt.kms;

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
}