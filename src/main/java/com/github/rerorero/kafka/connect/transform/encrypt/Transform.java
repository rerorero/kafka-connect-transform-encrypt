package com.github.rerorero.kafka.connect.transform.encrypt;

import com.github.rerorero.kafka.connect.transform.encrypt.config.Config;
import com.github.rerorero.kafka.connect.transform.encrypt.exception.ServerErrorException;
import com.github.rerorero.kafka.connect.transform.encrypt.exception.ServiceException;
import com.github.rerorero.kafka.connect.transform.encrypt.kms.CryptoConfig;
import com.github.rerorero.kafka.connect.transform.encrypt.kms.Item;
import com.github.rerorero.kafka.connect.transform.encrypt.kms.Service;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.transforms.Transformation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.apache.kafka.connect.transforms.util.Requirements.requireMap;
import static org.apache.kafka.connect.transforms.util.Requirements.requireStruct;

public abstract class Transform<R extends ConnectRecord<R>> implements Transformation<R> {
    private Service cryptoService;
    private Set<String> fields;
    private CryptoConfig cryptoConfig;

    @Override
    public ConfigDef config() {
        return Config.DEF;
    }

    @Override
    public void configure(Map<String, ?> props) {
        Config c = newConfig(props);
        this.cryptoService = c.cryptoService();
        this.fields = c.fields();
        this.cryptoConfig = c.cryptoCOnfig();
    }

    protected Config newConfig(Map<String, ?> props) {
        return new Config.ConfigImpl(props);
    }

    @Override
    public R apply(R record) {
        if (operatingSchema(record) == null) {
            return applySchemaless(record);
        } else {
            return applyWithSchema(record);
        }
    }

    @Override
    public void close() {
    }

    private R applySchemaless(R record) {
        final Map<String, Object> value = requireMap(operatingValue(record), "encrypt/decrypt");
        final HashMap<String, Object> updatedValue = new HashMap<>(value);

        Map<String, Item> params = new HashMap<>();
        for (String field : fields) {
            Object obj = value.get(field);
            if (obj != null) {
                Item item = itemFromEncodedObject(field, obj, cryptoConfig.getInputEncoding());
                params.put(field, item);
            }
        }

        if (params.isEmpty()) {
            return record;
        }

        Map<String, Item> results = doCrypto(params);

        results.forEach((field, item) -> {
            Object obj = item.asObject(cryptoConfig.getOutputEncoding());
            // TODO: update schema if needed
            updatedValue.put(field, obj);
        });

        return newRecord(record, updatedValue);
    }

    private R applyWithSchema(R record) {
        final Struct original = requireStruct(operatingValue(record), "encrypt/decrypt");
        final Struct updatedValue = new Struct(original.schema());

        Map<Field, Item> params = new HashMap<>();
        for (Field field: original.schema().fields()) {
            if (fields.contains(field.name())) {
                Object obj = original.get(field);
                if (obj != null) {
                    Item item = itemFromEncodedObject(field.name(), obj, cryptoConfig.getInputEncoding());
                    params.put(field, item);
                }
            }
        }

        if (params.isEmpty()) {
            return record;
        }

        Map<Field, Item> results = doCrypto(params);

        for (Field field: original.schema().fields()) {
            Item updated = results.get(field);
            if (updated != null) {
                Object obj = updated.asObject(cryptoConfig.getOutputEncoding());
                // TODO: update schema if needed
                updatedValue.put(field, obj);
            } else {
                updatedValue.put(field, original.get(field));
            }
        }

        return newRecord(record, updatedValue);
    }

    private Item itemFromEncodedObject(String name, Object obj, Item.Encoding encoding) {
        try {
            return Item.fromEncodedObject(obj, encoding);
        } catch (ClassCastException e) {
            throw new DataException("Failed to read '" + name + "' field as " + encoding.toString() + ": " + e.getMessage());
        }
    }

    private <F> Map<F, Item> doCrypto(Map<F, Item> params) {
        try {
            return cryptoService.doCrypto(params);
        } catch (ServerErrorException e) {
            throw new RetriableException(e);
        } catch (ServiceException e) {
            throw new DataException(e);
        }
    }

    abstract Schema operatingSchema(R record);

    abstract Object operatingValue(R record);

    abstract R newRecord(R base, Object value);

    static class Key<R extends ConnectRecord<R>> extends Transform<R> {
        @Override
        protected Schema operatingSchema(R record) {
            return record.keySchema();
        }

        @Override
        protected Object operatingValue(R record) {
            return record.key();
        }

        @Override
        protected R newRecord(R record, Object updatedValue) {
            return record.newRecord(record.topic(), record.kafkaPartition(), record.keySchema(), updatedValue, record.valueSchema(), record.value(), record.timestamp());
        }
    }

    static class Value<R extends ConnectRecord<R>> extends Transform<R> {
        @Override
        protected Schema operatingSchema(R record) {
            return record.valueSchema();
        }

        @Override
        protected Object operatingValue(R record) {
            return record.value();
        }

        @Override
        protected R newRecord(R record, Object updatedValue) {
            return record.newRecord(record.topic(), record.kafkaPartition(), record.keySchema(), record.key(), record.valueSchema(), updatedValue, record.timestamp());
        }
    }
}