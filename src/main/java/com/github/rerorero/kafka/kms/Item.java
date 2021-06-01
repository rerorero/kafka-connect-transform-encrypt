package com.github.rerorero.kafka.kms;

import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

public abstract class Item {
    public enum Encoding {
        STRING,
        BINARY,
    }

    public Object asObject(Encoding encoding) {
        switch (encoding) {
            case STRING:
                return asString();
            case BINARY:
                return asBytes();
        }
        return asString();
    }

    protected abstract String asString();

    protected abstract byte[] asBytes();

    static public class CipherBytes extends Item {
        private final byte[] value;

        public CipherBytes(byte[] value) {
            this.value = value;
        }

        @Override
        protected String asString() {
            return Base64.getEncoder().encodeToString(value);
        }

        @Override
        protected byte[] asBytes() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CipherBytes that = (CipherBytes) o;
            return Arrays.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }
    }

    public static class CipherText extends Item {
        private final String value;

        public CipherText(String value) {
            this.value = value;
        }

        @Override
        protected String asString() {
            return value;
        }

        @Override
        protected byte[] asBytes() {
            return value.getBytes();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CipherText that = (CipherText) o;
            return Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    public static class PlainBytes extends Item {
        private final byte[] value;

        public PlainBytes(byte[] value) {
            this.value = value;
        }

        @Override
        protected String asString() {
            return new String(value);
        }

        @Override
        protected byte[] asBytes() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PlainBytes that = (PlainBytes) o;
            return Arrays.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }
    }
}
