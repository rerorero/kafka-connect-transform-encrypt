package com.github.rerorero.kafka.connect.transform.encrypt;

import com.github.rerorero.kafka.connect.transform.encrypt.config.Config;
import com.github.rerorero.kafka.connect.transform.encrypt.config.FieldSelector;
import com.github.rerorero.kafka.connect.transform.encrypt.exception.ClientErrorException;
import com.github.rerorero.kafka.connect.transform.encrypt.exception.ServerErrorException;
import com.github.rerorero.kafka.connect.transform.encrypt.kms.CryptoConfig;
import com.github.rerorero.kafka.connect.transform.encrypt.kms.Item;
import com.github.rerorero.kafka.connect.transform.encrypt.kms.Service;
import com.github.rerorero.kafka.connect.transform.encrypt.util.Pair;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.sink.SinkRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

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

    private static final Schema STRUCT_SCHEMA = SchemaBuilder.struct()
            .field("array", SchemaBuilder.array(Schema.STRING_SCHEMA))
            .build();

    private static final Schema SCHEMA = SchemaBuilder.struct()
            .field("text", Schema.STRING_SCHEMA)
            .field("optional", SchemaBuilder.string().optional())
            .field("struct", STRUCT_SCHEMA)
            .build();

    private static Struct newStruct() {
        Struct sub = new Struct(STRUCT_SCHEMA);
        sub.put("array", Arrays.asList("PLAIN_ELEMENT1", "PLAIN_ELEMENT2"));

        Struct s = new Struct(SCHEMA);
        s.put("text", "PLAINTEXT");
        s.put("struct", sub);
        return s;
    }

    private static Map<String, Object> newMap() {
        Map<String, Object> m = new HashMap();
        m.put("byte", "plain".getBytes());
        m.put("struct", new HashMap<String, Object>() {{
            put("array", Arrays.asList(
                    "PLAIN_ELEMENT1".getBytes(),
                    "PLAIN_ELEMENT2".getBytes()
            ));
        }});
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
            public FieldSelector fieldSelector() {
                return newFieldSelector(new HashSet<>(fieldList));
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
    public void testApplyWithSchemaTextUsingJsonPath() {
        Transform sut = setUp(Arrays.asList("$.text", "$.struct.array[*]", "$.unknown"), Item.Encoding.STRING);

        Map<Pair<String, String>, Item> mockedResult = new HashMap<>();
        mockedResult.put(new Pair("$.text", "$.text"), new Item.StringItem("encrypted_text"));
        mockedResult.put(new Pair("$.struct.array[*]", "$.struct.array[0]"), new Item.StringItem("encrypted_array1"));
        mockedResult.put(new Pair("$.struct.array[*]", "$.struct.array[1]"), new Item.StringItem("encrypted_array2"));
        ArgumentCaptor<Map<Pair<String, String>, Item>> mockArg = ArgumentCaptor.forClass(Map.class);
        when(mockedService.doCrypto(ArgumentMatchers.<Map<Pair<String, String>, Item>>any())).thenReturn(mockedResult);

        Struct actual = (Struct) sut.apply(record(SCHEMA, newStruct())).value();

        verify(mockedService).doCrypto(mockArg.capture());
        Map<Pair<String, String>, Item> expectedMockArg = new HashMap<>();
        expectedMockArg.put(new Pair("$.text", "$.text"), new Item.StringItem("PLAINTEXT"));
        expectedMockArg.put(new Pair("$.struct.array[*]", "$.struct.array[0]"), new Item.StringItem("PLAIN_ELEMENT1"));
        expectedMockArg.put(new Pair("$.struct.array[*]", "$.struct.array[1]"), new Item.StringItem("PLAIN_ELEMENT2"));
        assertEquals(expectedMockArg, mockArg.getValue());

        Struct expected = newStruct();
        expected.put("text", "encrypted_text");
        expected.getStruct("struct").getArray("array").set(0, "encrypted_array1");
        expected.getStruct("struct").getArray("array").set(1, "encrypted_array2");
        assertEquals(expected, actual);
        assertNull(actual.schema().field("unknown"));
    }

    @Test
    public void testApplyWithoutSchemaBinaryUsingJsonPath() {
        Transform sut = setUp(Arrays.asList("$.byte", "$.struct.array[*]", "$.unknown"), Item.Encoding.BINARY);

        Map<Pair<String, String>, Item> mockedResult = new HashMap<>();
        mockedResult.put(new Pair("$.byte", "$.byte"), new Item.BytesItem("encrypted".getBytes()));
        mockedResult.put(new Pair("$.struct.array[*]", "$.struct.array[0]"), new Item.BytesItem("encrypted_binary1".getBytes()));
        mockedResult.put(new Pair("$.struct.array[*]", "$.struct.array[1]"), new Item.BytesItem("encrypted_binary2".getBytes()));
        ArgumentCaptor<Map<Pair<String, String>, Item>> mockArg = ArgumentCaptor.forClass(Map.class);
        when(mockedService.doCrypto(ArgumentMatchers.<Map<Pair<String, String>, Item>>any())).thenReturn(mockedResult);

        Map<String, Object> actual = (Map<String, Object>) sut.apply(record(null, newMap())).value();

        verify(mockedService).doCrypto(mockArg.capture());
        Map<Pair<String, String>, Item> expectedMockArg = new HashMap<>();
        expectedMockArg.put(new Pair("$.byte", "$.byte"), new Item.BytesItem("plain".getBytes()));
        expectedMockArg.put(new Pair("$.struct.array[*]", "$.struct.array[0]"), new Item.BytesItem("PLAIN_ELEMENT1".getBytes()));
        expectedMockArg.put(new Pair("$.struct.array[*]", "$.struct.array[1]"), new Item.BytesItem("PLAIN_ELEMENT2".getBytes()));
        assertEquals(expectedMockArg, mockArg.getValue());

        assertArrayEquals("encrypted".getBytes(), (byte[]) actual.get("byte"));
        assertArrayEquals("encrypted_binary1".getBytes(), ((List<byte[]>) ((Map<String, Object>) actual.get("struct")).get("array")).get(0));
        assertArrayEquals("encrypted_binary2".getBytes(), ((List<byte[]>) ((Map<String, Object>) actual.get("struct")).get("array")).get(1));
        assertNull(actual.get("unknown"));
    }

    @Test
    public void testApplyWithNoTargetColumn() {
        Transform sut = setUp(Arrays.asList("$.optional"), Item.Encoding.STRING);

        Struct actual = (Struct) sut.apply(record(SCHEMA, newStruct())).value();

        Struct expected = newStruct();
        assertEquals(expected, actual);
    }

    @Test
    public void testFailureWithInvalidJsonPath() {
        assertThrows(ConfigException.class, () -> setUp(Arrays.asList("text"), Item.Encoding.STRING));
    }

    @Test
    public void testFailureWithServiceServerError() {
        Transform sut = setUp(Arrays.asList("$.text"), Item.Encoding.STRING);
        when(mockedService.doCrypto(ArgumentMatchers.<Map<Pair<String, String>, Item>>any())).thenThrow(new ServerErrorException("fail"));
        assertThrows(RetriableException.class, () -> sut.apply(record(SCHEMA, newStruct())));
    }

    @Test
    public void testFailureWithServiceClientError() {
        Transform sut = setUp(Arrays.asList("$.text"), Item.Encoding.STRING);
        when(mockedService.doCrypto(ArgumentMatchers.<Map<Pair<String, String>, Item>>any())).thenThrow(new ClientErrorException("fail"));
        assertThrows(DataException.class, () -> sut.apply(record(SCHEMA, newStruct())));
    }

    @Test
    public void testInvalidEncodingErrorWithSchema() {
        Transform sut = setUp(Arrays.asList("$.text"), Item.Encoding.BINARY);
        assertThrows(DataException.class, () -> sut.apply(record(SCHEMA, newStruct())));
    }

    @Test
    public void testInvalidEncodingErrorWithSchemaless() {
        Transform sut = setUp(Arrays.asList("$.byte"), Item.Encoding.STRING);
        assertThrows(DataException.class, () -> sut.apply(record(null, newMap())));
    }
}