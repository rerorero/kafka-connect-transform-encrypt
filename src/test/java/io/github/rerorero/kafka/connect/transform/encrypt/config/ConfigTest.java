package io.github.rerorero.kafka.connect.transform.encrypt.config;

import io.github.rerorero.kafka.aws.AWSKeyManagementService;
import io.github.rerorero.kafka.vault.VaultService;
import org.apache.kafka.common.config.ConfigException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    public void testConfigAWSKMS() {
        Map<String, Object> props = new HashMap<>();
        props.put(Config.SERVICE, "awskms");
        props.put(Config.MODE, Config.MODE_DECRYPT);
        props.put(Config.AWSKMS_CMK_KEYID, "arn:aws:kms:eu-west-2:111122223333:key/c2bf6ecc-def2-4036-86b0-ba3e73fdbcf9");
        Config conf = new Config.ConfigImpl(props);
        assertTrue(conf.cryptoService() instanceof AWSKeyManagementService.DecryptService);
    }

    @Test
    public void testFailWithInvalidConfig() {
        Map<String, Object> props = new HashMap<>();
        assertThrows(ConfigException.class, () -> new Config.ConfigImpl(props));
    }
}