package com.github.rerorero.kafka.connect.transform.encrypt.kms;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

public abstract class Item {
    public enum Encoding {
        STRING,
        BINARY,
        BASE64STRING,
    }

    public static Item fromEncodedObject(Object item, Encoding encoding) {
        switch (encoding) {
            case STRING:
                return new StringItem((String) item);
            case BINARY:
                return new BytesItem((byte[]) item);
            case BASE64STRING:
                return new Base64StringItem((String) item);
        }
        return new StringItem((String) item);
    }

    public static Item fromString(String text, Encoding enc) {
        switch (enc) {
            case STRING:
                return new StringItem(text);
            case BINARY:
                return new BytesItem(s2b(text));
            case BASE64STRING:
                return new Base64StringItem(b2s(Base64.getEncoder().encode(s2b(text))));
        }
        return new StringItem(text);
    }

    public static Item fromBase64(String base64text, Encoding enc) {
        switch (enc) {
            case STRING:
                return new StringItem(new String(Base64.getDecoder().decode(base64text), Charset.defaultCharset()));
            case BINARY:
                return new BytesItem(Base64.getDecoder().decode(base64text));
            case BASE64STRING:
                return new Base64StringItem(base64text);
        }
        return new Base64StringItem(base64text);
    }

    public Object asObject(Encoding encoding) {
        switch (encoding) {
            case STRING:
                return asText();
            case BINARY:
                return asBytes();
            case BASE64STRING:
                return asBase64String();
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

    // Base64 string
    public static class Base64StringItem extends Item {
        private final String item;

        public Base64StringItem(String item) {
            this.item = item;
        }

        @Override
        public String asBase64String() {
            return item;
        }

        @Override
        public byte[] asBytes() {
            return Base64.getDecoder().decode(item);
        }

        @Override
        public String asText() {
            return new String(asBytes(), Charset.defaultCharset());
        }

        @Override
        public String toString() {
            return "Base64StringItem{" +
                    "item='" + item + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Base64StringItem that = (Base64StringItem) o;
            return Objects.equals(item, that.item);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item);
        }
    }
}
