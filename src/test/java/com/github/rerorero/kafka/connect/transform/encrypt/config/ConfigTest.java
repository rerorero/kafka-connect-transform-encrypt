package com.github.rerorero.kafka.connect.transform.encrypt.config;

import com.github.rerorero.kafka.vault.VaultService;
import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {
    @Test
    public void testConfigVault() {
        Map<String, Object> props = new HashMap<>();
        props.put(Config.SERVICE, "vault");
        props.put(Config.MODE, Config.MODE_ENCRYPT);
        props.put(Config.VAULT_URL, "http://localhost");
        props.put(Config.VAULT_KEY_NAME, "mykey");
        Config conf = new Config.ConfigImpl(props);
        assertTrue(conf.cryptoService() instanceof VaultService.EncryptService);
    }

    @Test
    public void testFailWithInvalidConfig() {
        Map<String, Object> props = new HashMap<>();
        assertThrows(ConfigException.class, () -> new Config.ConfigImpl(props));
    }
}