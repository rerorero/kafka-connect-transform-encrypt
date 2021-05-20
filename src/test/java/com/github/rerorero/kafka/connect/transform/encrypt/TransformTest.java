package com.github.rerorero.kafka.connect.transform.encrypt;

import com.github.rerorero.kafka.connect.transform.encrypt.config.Config;
import com.github.rerorero.kafka.connect.transform.encrypt.exception.ClientErrorException;
import com.github.rerorero.kafka.connect.transform.encrypt.exception.ServerErrorException;
import com.github.rerorero.kafka.connect.transform.encrypt.kms.CryptoConfig;
import com.github.rerorero.kafka.connect.transform.encrypt.kms.Item;
import com.github.rerorero.kafka.connect.transform.encrypt.kms.Service;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.nio.charset.Charset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransformTest {
    private static class Mocked extends Transform.Value<SinkRecord> {
        private final Config config;

        Mocked(Config config) {
            this.config = config;
        }

        @Override
        protected Config newConfig(Map<String, ?> props) {
            return config;
        }
    }

    private static final Schema SCHEMA = SchemaBuilder.struct()
            .field("text", Schema.STRING_SCHEMA)
            .field("text_opt", SchemaBuilder.string().optional().build())
            .field("text_null", SchemaBuilder.string().optional().build())
            .field("byte", Schema.BYTES_SCHEMA)
            .field("byte_opt", SchemaBuilder.bytes().optional().build())
            .field("byte_null", SchemaBuilder.bytes().optional().build())
            .build();

    private static Struct newStruct() {
        Struct s = new Struct(SCHEMA);
        s.put("text", "PLAINTEXT");
        s.put("text_opt", "PLAIN_OPTIONAL_TEXT");
        s.put("byte", "PLAINBYTE".getBytes(Charset.defaultCharset()));
        s.put("byte_opt", "PLAIN_OPTIONAL_BYTE".getBytes(Charset.defaultCharset()));
        return s;
    }

    private static Map<String, Object> newMap() {
        Map<String, Object> m = new HashMap();
        m.put("text", "PLAINTEXT");
        m.put("text_opt", "PLAIN_OPTIONAL_TEXT");
        m.put("byte", "PLAINBYTE".getBytes(Charset.defaultCharset()));
        m.put("byte_opt", "PLAIN_OPTIONAL_BYTE".getBytes(Charset.defaultCharset()));
        return m;
    }

    private static SinkRecord record(Schema schema, Object value) {
        return new SinkRecord("", 0, null, null, schema, value, 0);
    }

    private Service mockedService;

    private Transform<SinkRecord> setUp(List<String> fieldList, Item.Encoding enc) {
        this.mockedService = mock(Service.class);
        Set<String> fields = new HashSet<>(fieldList);
        CryptoConfig cryptoConf = new CryptoConfig(enc, enc);
        Config config = new Config() {
            @Override
            public Service cryptoService() {
                return mockedService;
            }

            @Override
            public Set<String> fields() {
                return fields;
            }

            @Override
            public CryptoConfig cryptoCOnfig() {
                return cryptoConf;
            }
        };
        Transform sut = new Mocked(config);
        sut.configure(null);
        return sut;
    }

    @Test
    public void testApplyWithSchemaText() {
        Transform sut = setUp(Arrays.asList("text", "text_opt", "text_null", "unknown"), Item.Encoding.STRING);

        Map<Field, Item> mockedResult = new HashMap<>();
        mockedResult.put(SCHEMA.field("text"), new Item.StringItem("encrypted_text"));
        mockedResult.put(SCHEMA.field("text_opt"), new Item.StringItem("encrypted_optional_text"));
        ArgumentCaptor<Map<Field, Item>> mockArg = ArgumentCaptor.forClass(Map.class);
        when(mockedService.doCrypto(ArgumentMatchers.<Map<Field, Item>>any())).thenReturn(mockedResult);

        Struct actual = (Struct) sut.apply(record(SCHEMA, newStruct())).value();

        verify(mockedService).doCrypto(mockArg.capture());
        Map<Field, Item> expectedMockArg = new HashMap<>();
        expectedMockArg.put(SCHEMA.field("text"), new Item.StringItem("PLAINTEXT"));
        expectedMockArg.put(SCHEMA.field("text_opt"), new Item.StringItem("PLAIN_OPTIONAL_TEXT"));
        assertEquals(expectedMockArg, mockArg.getValue());

        assertEquals("encrypted_text", actual.get("text"));
        assertEquals("encrypted_optional_text", actual.get("text_opt"));
        assertNull(actual.get("text_null"));
        assertArrayEquals("PLAINBYTE".getBytes(Charset.defaultCharset()), (byte[]) actual.get("byte"));
        assertArrayEquals("PLAIN_OPTIONAL_BYTE".getBytes(Charset.defaultCharset()), (byte[]) actual.get("byte_opt"));
        assertNull(actual.get("byte_null"));
        assertNull(actual.schema().field("unknown"));
    }

    @Test
    public void testApplyWithSchemaBytes() {
        Transform sut = setUp(Arrays.asList("byte", "byte_opt", "byte_null", "unknown"), Item.Encoding.BINARY);

        Map<Field, Item> mockedResult = new HashMap<>();
        mockedResult.put(SCHEMA.field("byte"), new Item.BytesItem("encrypted_bytes".getBytes(Charset.defaultCharset())));
        mockedResult.put(SCHEMA.field("byte_opt"), new Item.BytesItem("encrypted_optional_bytes".getBytes(Charset.defaultCharset())));
        ArgumentCaptor<Map<Field, Item>> mockArg = ArgumentCaptor.forClass(Map.class);
        when(mockedService.doCrypto(ArgumentMatchers.<Map<Field, Item>>any())).thenReturn(mockedResult);

        Struct actual = (Struct) sut.apply(record(SCHEMA, newStruct())).value();

        verify(mockedService).doCrypto(mockArg.capture());
        Map<Field, Item> expectedMockArg = new HashMap<>();
        expectedMockArg.put(SCHEMA.field("byte"), new Item.BytesItem("PLAINBYTE".getBytes(Charset.defaultCharset())));
        expectedMockArg.put(SCHEMA.field("byte_opt"), new Item.BytesItem("PLAIN_OPTIONAL_BYTE".getBytes(Charset.defaultCharset())));
        assertEquals(expectedMockArg, mockArg.getValue());

        assertEquals("PLAINTEXT", actual.get("text"));
        assertEquals("PLAIN_OPTIONAL_TEXT", actual.get("text_opt"));
        assertNull(actual.get("text_null"));
        assertArrayEquals("encrypted_bytes".getBytes(Charset.defaultCharset()), (byte[]) actual.get("byte"));
        assertArrayEquals("encrypted_optional_bytes".getBytes(Charset.defaultCharset()), (byte[]) actual.get("byte_opt"));
        assertNull(actual.get("byte_null"));
        assertNull(actual.schema().field("unknown"));
    }

    @Test
    public void testApplyWithoutSchemaString() {
        Transform sut = setUp(Arrays.asList("text", "text_opt", "text_null", "unknown"), Item.Encoding.STRING);

        Map<String, Item> mockedResult = new HashMap<>();
        mockedResult.put("text", new Item.StringItem("encrypted_text"));
        mockedResult.put("text_opt", new Item.StringItem("encrypted_optional_text"));
        ArgumentCaptor<Map<String, Item>> mockArg = ArgumentCaptor.forClass(Map.class);
        when(mockedService.doCrypto(ArgumentMatchers.<Map<String, Item>>any())).thenReturn(mockedResult);

        Map<String, Object> actual = (Map) sut.apply(record(null, newMap())).value();

        verify(mockedService).doCrypto(mockArg.capture());
        Map<String, Item> expectedMockArg = new HashMap<>();
        expectedMockArg.put("text", new Item.StringItem("PLAINTEXT"));
        expectedMockArg.put("text_opt", new Item.StringItem("PLAIN_OPTIONAL_TEXT"));
        assertEquals(expectedMockArg, mockArg.getValue());

        assertEquals("encrypted_text", actual.get("text"));
        assertEquals("encrypted_optional_text", actual.get("text_opt"));
        assertNull(actual.get("text_null"));
        assertArrayEquals("PLAINBYTE".getBytes(Charset.defaultCharset()), (byte[]) actual.get("byte"));
        assertArrayEquals("PLAIN_OPTIONAL_BYTE".getBytes(Charset.defaultCharset()), (byte[]) actual.get("byte_opt"));
        assertNull(actual.get("byte_null"));
        assertNull(actual.get("unknown"));
    }

    @Test
    public void testApplyWithoutSchemaBytes() {
        Transform sut = setUp(Arrays.asList("byte", "byte_opt", "byte_null", "unknown"), Item.Encoding.BINARY);

        Map<String, Item> mockedResult = new HashMap<>();
        mockedResult.put("byte", new Item.BytesItem("encrypted_bytes".getBytes(Charset.defaultCharset())));
        mockedResult.put("byte_opt", new Item.BytesItem("encrypted_optional_bytes".getBytes(Charset.defaultCharset())));
        ArgumentCaptor<Map<String, Item>> mockArg = ArgumentCaptor.forClass(Map.class);
        when(mockedService.doCrypto(ArgumentMatchers.<Map<String, Item>>any())).thenReturn(mockedResult);

        Map<String, Object> actual = (Map) sut.apply(record(null, newMap())).value();

        verify(mockedService).doCrypto(mockArg.capture());
        Map<String, Item> expectedMockArg = new HashMap<>();
        expectedMockArg.put("byte", new Item.BytesItem("PLAINBYTE".getBytes(Charset.defaultCharset())));
        expectedMockArg.put("byte_opt", new Item.BytesItem("PLAIN_OPTIONAL_BYTE".getBytes(Charset.defaultCharset())));
        assertEquals(expectedMockArg, mockArg.getValue());

        assertEquals("PLAINTEXT", actual.get("text"));
        assertEquals("PLAIN_OPTIONAL_TEXT", actual.get("text_opt"));
        assertNull(actual.get("text_null"));
        assertArrayEquals("encrypted_bytes".getBytes(Charset.defaultCharset()), (byte[]) actual.get("byte"));
        assertArrayEquals("encrypted_optional_bytes".getBytes(Charset.defaultCharset()), (byte[]) actual.get("byte_opt"));
        assertNull(actual.get("byte_null"));
        assertNull(actual.get("unknown"));
    }

    @Test
    public void testFailureWithServiceServerError() {
        Transform sut = setUp(Arrays.asList("text"), Item.Encoding.STRING);
        when(mockedService.doCrypto(ArgumentMatchers.<Map<String, Item>>any())).thenThrow(new ServerErrorException("fail"));
        assertThrows(RetriableException.class, () -> sut.apply(record(null, newMap())));
    }

    @Test
    public void testFailureWithServiceClientError() {
        Transform sut = setUp(Arrays.asList("text"), Item.Encoding.STRING);
        when(mockedService.doCrypto(ArgumentMatchers.<Map<String, Item>>any())).thenThrow(new ClientErrorException("fail"));
        assertThrows(DataException.class, () -> sut.apply(record(null, newMap())));
    }

    @Test
    public void testInvalidEncodingErrorWithSchema() {
        Transform sut = setUp(Arrays.asList("byte"), Item.Encoding.STRING);
        assertThrows(DataException.class, () -> sut.apply(record(SCHEMA, newStruct())));
    }

    @Test
    public void testInvalidEncodingErrorWithSchemaless() {
        Transform sut = setUp(Arrays.asList("text"), Item.Encoding.BINARY);
        assertThrows(DataException.class, () -> sut.apply(record(null, newMap())));
    }
}