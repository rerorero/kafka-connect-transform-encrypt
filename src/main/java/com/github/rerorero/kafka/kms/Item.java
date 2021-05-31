package com.github.rerorero.kafka.kms;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

public abstract class Item {
    public enum Encoding {
        STRING,
        BINARY,
    }

    public static Item fromBase64(String base64text) {
        return new BytesItem(Base64.getDecoder().decode(base64text));
    }

    public Object asObject(Encoding encoding) {
        switch (encoding) {
            case STRING:
                return asText();
            case BINARY:
                return asBytes();
        }
        return asText();
    }

    protected static String b2s(byte[] b) {
        return new String(b, Charset.defaultCharset());
    }

    protected static byte[] s2b(String s) {
        return s.getBytes(Charset.defaultCharset());
    }

    public abstract byte[] asBytes();

    public abstract String asText();

    public String asBase64String() {
        return new String(Base64.getEncoder().encode(asBytes()));
    }

    // String biased item
    public static class StringItem extends Item {
        private final String item;

        public StringItem(String item) {
            this.item = item;
        }

        @Override
        public byte[] asBytes() {
            return s2b(item);
        }

        @Override
        public String asText() {
            return item;
        }

        @Override
        public String toString() {
            return "StringItem{" +
                    "item='" + item + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StringItem that = (StringItem) o;
            return Objects.equals(item, that.item);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item);
        }
    }

    // byte[] biased item
    public static class BytesItem extends Item {
        private final byte[] item;

        public BytesItem(byte[] item) {
            this.item = item;
        }

        @Override
        public byte[] asBytes() {
            return item;
        }

        @Override
        public String asText() {
            return b2s(item);
        }

        @Override
        public String toString() {
            return "BytesItem{" +
                    "item=" + Arrays.toString(item) +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BytesItem bytesItem = (BytesItem) o;
            return Arrays.equals(item, bytesItem.item);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(item);
        }
    }
}
