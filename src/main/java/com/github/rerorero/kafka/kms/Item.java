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

    public static Item fromObject(Object item) {
        if (item instanceof String) {
            return new StringItem((String) item);
        } else if (item instanceof byte[]) {
            return new BytesItem((byte[]) item);
        }
        return new StringItem(item.toString());
    }
//
//    public static Item fromString(String text, Encoding enc) {
//        switch (enc) {
//            case STRING:
//                return new StringItem(text);
//            case BINARY:
//                return new BytesItem(s2b(text));
//        }
//        return new StringItem(text);
//    }
//
//    public static Item fromBytes(byte[] bytes, Encoding enc) {
//        switch (enc) {
//            case STRING:
//                return new StringItem(b2s(Base64.getEncoder().encode(bytes)));
//            case BINARY:
//                return new BytesItem(bytes);
//        }
//        return new BytesItem(bytes);
//    }

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
