package com.github.rerorero.kafka.vault;

import com.github.rerorero.kafka.connect.transform.encrypt.exception.ClientErrorException;
import com.github.rerorero.kafka.kms.Item;
import com.github.rerorero.kafka.kms.Service;
import com.github.rerorero.kafka.util.Pair;
import com.github.rerorero.kafka.vault.client.DecryptParameter;
import com.github.rerorero.kafka.vault.client.EncryptParameter;
import com.github.rerorero.kafka.vault.client.VaultClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

public abstract class VaultService<Param> implements Service {
    private final Logger log = LoggerFactory.getLogger(VaultService.class);

    protected final VaultClient client;
    protected final VaultCryptoConfig config;

    public VaultService(VaultClient client, VaultCryptoConfig config) {
        this.client = client;
        this.config = config;
    }

    protected abstract Param newParameter(Object item);

    protected abstract Item newItemResult(String result);

    protected abstract List<String> invokeCrypto(List<Param> params);

    @Override
    public void init() {
    }

    @Override
    public void close() {
    }

    @Override
    public <F> Map<F, Item> doCrypto(Map<F, Object> items) {
        List<Pair<F, Object>> list = new ArrayList<>();
        items.forEach((key, item) -> {
            list.add(new Pair(key, item));
        });

        List<Param> params = list.stream()
                .map(pair -> newParameter(pair.value))
                .collect(Collectors.toList());

        List<String> res = invokeCrypto(params);

        Map<F, Item> results = new HashMap<>();
        for (int i = 0; i < res.size(); i++) {
            results.put(list.get(i).key, newItemResult(res.get(i)));
        }

        return results;
    }

    public static final class EncryptService extends VaultService<EncryptParameter> {
        public EncryptService(VaultClient client, VaultCryptoConfig config) {
            super(client, config);
        }

        @Override
        protected EncryptParameter newParameter(Object item) {
            String base64Text;
            if (item instanceof String) {
                String s = (String) item;
                base64Text = Base64.getEncoder().encodeToString(s.getBytes(Charset.defaultCharset()));
            } else if (item instanceof byte[]) {
                base64Text = Base64.getEncoder().encodeToString((byte[]) item);
            } else {
                throw new ClientErrorException("not supported field type: " + item.getClass());
            }

            return new EncryptParameter(base64Text, config.getContext());
        }

        @Override
        protected Item newItemResult(String result) {
            return new Item.CipherText(result);
        }

        @Override
        protected List<String> invokeCrypto(List<EncryptParameter> params) {
            return client.encrypt(config.getKeyName(), params);
        }
    }

    public static final class DecryptService extends VaultService<DecryptParameter> {
        public DecryptService(VaultClient client, VaultCryptoConfig config) {
            super(client, config);
        }

        @Override
        protected DecryptParameter newParameter(Object item) {
            String text;
            if (item instanceof String) {
                text = (String) item;
            } else if (item instanceof byte[]) {
                text = new String((byte[]) item, Charset.defaultCharset());
            } else {
                throw new ClientErrorException("not supported field type: " + item.getClass());
            }
            return new DecryptParameter(text, config.getContext());
        }

        @Override
        protected Item newItemResult(String result) {
            byte[] bytes = Base64.getDecoder().decode(result);
            ;
            return new Item.PlainBytes(bytes);
        }

        @Override
        protected List<String> invokeCrypto(List<DecryptParameter> params) {
            return client.decrypt(config.getKeyName(), params);
        }
    }
}
