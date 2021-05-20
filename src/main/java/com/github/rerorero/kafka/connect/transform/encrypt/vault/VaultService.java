package com.github.rerorero.kafka.connect.transform.encrypt.vault;

import com.github.rerorero.kafka.connect.transform.encrypt.kms.Item;
import com.github.rerorero.kafka.connect.transform.encrypt.kms.Service;
import com.github.rerorero.kafka.connect.transform.encrypt.util.Pair;
import com.github.rerorero.kafka.connect.transform.encrypt.vault.client.DecryptParameter;
import com.github.rerorero.kafka.connect.transform.encrypt.vault.client.EncryptParameter;
import com.github.rerorero.kafka.connect.transform.encrypt.vault.client.VaultClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class VaultService<Param> implements Service {
    private final Logger log = LoggerFactory.getLogger(VaultService.class);

    protected final VaultClient client;
    protected final VaultCryptoConfig config;

    public VaultService(VaultClient client, VaultCryptoConfig config) {
        this.client = client;
        this.config = config;
    }

    protected abstract Param newParameter(Item item);

    protected abstract Item newItemResult(String result);

    protected abstract List<String> invokeCrypto(List<Param> params);

    @Override
    public <F> Map<F, Item> doCrypto(Map<F, Item> items) {
        List<Pair<F, Item>> list = new ArrayList<>();
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
        protected EncryptParameter newParameter(Item item) {
            return new EncryptParameter(item.asBase64String(), config.getContext());
        }

        @Override
        protected Item newItemResult(String result) {
            return Item.fromString(result, config.getOutputEncoding());
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
        protected DecryptParameter newParameter(Item item) {
            return new DecryptParameter(item.asText(), config.getContext());
        }

        @Override
        protected Item newItemResult(String result) {
            return Item.fromBase64(result, config.getOutputEncoding());
        }

        @Override
        protected List<String> invokeCrypto(List<DecryptParameter> params) {
            return client.decrypt(config.getKeyName(), params);
        }
    }
}
