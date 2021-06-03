package com.github.rerorero.kafka.connect.transform.encrypt;

import com.github.rerorero.kafka.connect.transform.encrypt.condition.Condition;
import com.github.rerorero.kafka.connect.transform.encrypt.condition.Conditions;
import com.github.rerorero.kafka.connect.transform.encrypt.config.Config;
import com.github.rerorero.kafka.connect.transform.encrypt.config.FieldSelector;
import com.github.rerorero.kafka.connect.transform.encrypt.exception.ServerErrorException;
import com.github.rerorero.kafka.connect.transform.encrypt.exception.ServiceException;
import com.github.rerorero.kafka.kms.CryptoConfig;
import com.github.rerorero.kafka.kms.Item;
import com.github.rerorero.kafka.kms.Service;
import com.github.rerorero.kafka.util.Pair;
import com.github.rerorero.kafka.jsonpath.JsonPath;
import com.github.rerorero.kafka.jsonpath.JsonPathException;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.ConnectRecord;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.errors.RetriableException;
import org.apache.kafka.connect.transforms.Transformation;

import java.util.HashMap;
import java.util.Map;

import static org.apache.kafka.connect.transforms.util.Requirements.requireMap;
import static org.apache.kafka.connect.transforms.util.Requirements.requireStruct;

public abstract class Transform<R extends ConnectRecord<R>> implements Transformation<R> {
    private Service cryptoService;
    private CryptoConfig cryptoConfig;
    private FieldSelector fieldSelector;
    private Conditions conditions;

    @Override
    public ConfigDef config() {
        return Config.DEF;
    }

    @Override
    public void configure(Map<String, ?> props) {
        Config c = newConfig(props);
        this.cryptoService = c.cryptoService();
        this.fieldSelector = c.fieldSelector();
        this.cryptoConfig = c.cryptoCOnfig();
        this.conditions = c.conditions();
    }

    protected Config newConfig(Map<String, ?> props) {
        return new Config.ConfigImpl(props);
    }

    @Override
    public R apply(R record) {
        Object updated = null;
        if (operatingSchema(record) == null) {
            final Map<String, Object> org = requireMap(operatingValue(record), "encrypt/decrypt");
            updated = doCrypto(org, fieldSelector.mapGetters, fieldSelector.mapUpdaters, conditions.mapCondition);
        } else {
            final Struct org = requireStruct(operatingValue(record), "encrypt/decrypt");
            updated = doCrypto(org, fieldSelector.structGetters, fieldSelector.structUpdaters, conditions.structCondition);
        }
        return newRecord(record, updated);
    }

    @Override
    public void close() {
        if (cryptoService != null) {
            cryptoService.close();
        }
    }

    private <R> R doCrypto(
            R value,
            Map<String, JsonPath.Getter<R>> getters,
            Map<String, JsonPath.Updater<R>> updaters,
            Condition<R> condition
    ) {
        try {
            if (!condition.accept(value)) {
                return value;
            }

            // Key of the map is a pair of JsonPath expression and the field path
            final Map<Pair<String, String>, Object> params = new HashMap<>();
            getters.forEach((jsonPathExp, getter) ->
                    getter.run(value).forEach((fieldPath, fieldValue) ->
                            params.put(new Pair(jsonPathExp, fieldPath), fieldValue)
                    )
            );

            if (params.isEmpty()) {
                return value;
            }

            Map<Pair<String, String>, Item> results = null;
            results = cryptoService.doCrypto(params);

            // Map from JsonPath expression to encrypted/decrypted values which is a map from field path to the new value.
            final HashMap<String, Map<String, Object>> newValues = new HashMap<>();
            results.forEach((pair, item) -> {
                String jsonPathExp = pair.key;
                String fieldPath = pair.value;
                Object newVal = item.asObject(cryptoConfig.getOutputEncoding());
                newValues.computeIfAbsent(jsonPathExp, exp -> new HashMap<>())
                        .put(fieldPath, newVal);
            });

            // Apply Updater for each JsonPath
            R updated = value;
            for (Map.Entry<String, Map<String, Object>> kv : newValues.entrySet()) {
                JsonPath.Updater<R> updater = updaters.get(kv.getKey());
                updated = updater.run(updated, kv.getValue());
            }

            return updated;
        } catch (ServerErrorException e) {
            throw new RetriableException(e);
        } catch (ServiceException e) {
            throw new DataException(e);
        } catch (JsonPathException e) {
            throw new DataException(e);
        }
    }

    abstract Schema operatingSchema(R record);

    abstract Object operatingValue(R record);

    abstract R newRecord(R base, Object value);

    public static class Key<R extends ConnectRecord<R>> extends Transform<R> {
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

    public static class Value<R extends ConnectRecord<R>> extends Transform<R> {
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