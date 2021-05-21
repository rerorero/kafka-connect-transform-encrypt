package com.github.rerorero.kafka.connect.transform.encrypt.vault.client;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.json.JsonArray;
import com.bettercloud.vault.json.JsonObject;
import com.bettercloud.vault.json.JsonValue;
import com.bettercloud.vault.response.LogicalResponse;
import com.bettercloud.vault.rest.RestResponse;
import com.github.rerorero.kafka.connect.transform.encrypt.exception.ClientErrorException;
import com.github.rerorero.kafka.connect.transform.encrypt.exception.ServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VaultClientImpl implements VaultClient {
    private final Logger log = LoggerFactory.getLogger(VaultClientImpl.class);

    private final Vault client;

    public VaultClientImpl(Vault client) {
        this.client = client;
    }

    @Override
    public List<String> encrypt(String keyName, List<EncryptParameter> items) {
        // ref. https://github.com/hashicorp/vault/blob/v1.7.1/builtin/logical/transit/path_encrypt.go#L17-L39
        final JsonArray itemsJson = new JsonArray();
        for (EncryptParameter i : items) {
            final JsonObject obj = new JsonObject().add("plaintext", i.plainTextBase64);
            i.context.ifPresent(c -> obj.add("context", c));
            i.keyVersion.ifPresent(version -> obj.add("key_version", version));
            itemsJson.add(obj);
        }

        return batchRequest("transit/encrypt/" + keyName, itemsJson, "ciphertext");
    }

    @Override
    public List<String> decrypt(String keyName, List<DecryptParameter> items) {
        // ref. https://github.com/hashicorp/vault/blob/v1.7.1/builtin/logical/transit/path_encrypt.go#L17-L39
        final JsonArray itemsJson = new JsonArray();
        for (DecryptParameter i : items) {
            final JsonObject obj = new JsonObject().add("ciphertext", i.cipherText);
            i.context.ifPresent(c -> obj.add("context", c));
            itemsJson.add(obj);
        }

        return batchRequest("transit/decrypt/" + keyName, itemsJson, "plaintext");
    }

    private List<String> batchRequest(String path, JsonArray batchInput, String outField) {
        if (batchInput.isEmpty()) {
            return new ArrayList<>();
        }

        log.debug("vault request: path={}, batch_input size={}", path, batchInput.size());

        LogicalResponse logicarlRes = null;
        try {
            logicarlRes = client.logical().write(path, Collections.singletonMap("batch_input", batchInput));
        } catch (VaultException e) {
            throw new ClientErrorException("Failed to access Vault", e);
        }

        final RestResponse res = logicarlRes.getRestResponse();

        log.debug("vault response: status={}", res.getStatus());

        if (res.getStatus() / 100 == 2) {
            final JsonArray results = logicarlRes.getDataObject().get("batch_results").asArray();
            if (results == null) {
                throw new ServerErrorException(String.format("Unexpected vault response: %s", new String(res.getBody())));
            }

            final List<String> batchResponses = new ArrayList<>();
            for (JsonValue r : results) {
                if (!r.isObject()) {
                    throw new ServerErrorException(String.format("Unexpected vault response: %s", new String(res.getBody())));
                }

                final JsonObject obj = r.asObject();
                if (obj.get("error") != null) {
                    throw new ClientErrorException("Vault respond error: " + obj.get("error"));
                }

                JsonValue out = obj.get(outField);
                if (out == null) {
                    throw new ServerErrorException(String.format("Unexpected vault response: %s", new String(res.getBody())));
                }

                batchResponses.add(out.asString());
            }

            return batchResponses;

        } else if (res.getStatus() / 100 == 4) {
            throw new ClientErrorException(String.format("Vault respond error: status=%d, %s",
                    res.getStatus(), new String(res.getBody())));
        } else {
            throw new ServerErrorException(String.format("Vault respond error: status=%d, %s",
                    res.getStatus(), new String(res.getBody())));
        }
    }
}
