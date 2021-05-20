package com.github.rerorero.kafka.connect.transform.encrypt.config;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.github.rerorero.kafka.connect.transform.encrypt.kms.CryptoConfig;
import com.github.rerorero.kafka.connect.transform.encrypt.kms.Item;
import com.github.rerorero.kafka.connect.transform.encrypt.kms.Service;
import com.github.rerorero.kafka.connect.transform.encrypt.vault.VaultCryptoConfig;
import com.github.rerorero.kafka.connect.transform.encrypt.vault.VaultService;
import com.github.rerorero.kafka.connect.transform.encrypt.vault.client.VaultClient;
import com.github.rerorero.kafka.connect.transform.encrypt.vault.client.VaultClientImpl;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.transforms.util.SimpleConfig;

import java.util.*;

public abstract class Config {
    // general configurations
    public static final String SERVICE = "service";
    public static final String SERVICE_VAULT = "vault";
    private static final OneOfValidator<String> serviceValidator = new OneOfValidator<>(SERVICE_VAULT);

    public static final String MODE = "mode";
    public static final String MODE_ENCRYPT = "encrypt";
    public static final String MODE_DECRYPT = "decrypt";
    private static final OneOfValidator<String> modeValidator = new OneOfValidator<>(MODE_ENCRYPT, MODE_DECRYPT);

    public static final String FIELDS = "fields";
    public static final String FIELD_ENCODING_IN = "field.encoding.in";
    public static final String FIELD_ENCODING_OUT = "field.encoding.out";
    public static final String FIELD_ENCODING_STRING = "string";
    public static final String FIELD_ENCODING_BINARY = "binary";
    public static final String FIELD_ENCODING_BASE64 = "base64";
    private static final OneOfValidator<String> encodingValidator = new OneOfValidator<>(FIELD_ENCODING_STRING, FIELD_ENCODING_BINARY, FIELD_ENCODING_BASE64);

    // Vault
    public static final String VAULT_URL = "vault.url";
    public static final String VAULT_TOKEN = "vault.token";
    public static final String VAULT_KEY_NAME = "vault.key_name";
    public static final String VAULT_CONTEXT = "vault.context";

    public static final ConfigDef DEF = new ConfigDef()
            // general
            .define(SERVICE, ConfigDef.Type.STRING, ConfigDef.NO_DEFAULT_VALUE, modeValidator,
                    ConfigDef.Importance.HIGH, "Name of the service that provides encryption.")
            .define(MODE, ConfigDef.Type.STRING, ConfigDef.NO_DEFAULT_VALUE, modeValidator,
                    ConfigDef.Importance.HIGH, "Specifies the mode, " + MODE_DECRYPT + " or " + MODE_DECRYPT + ".")
            .define(FIELDS, ConfigDef.Type.LIST, new ArrayList<String>(),
                    ConfigDef.Importance.HIGH, "Names of the fields to be encrypted or decrypted. "
                    + "Only string and byte field in the root are supported for now.")
            .define(FIELD_ENCODING_IN, ConfigDef.Type.STRING, FIELD_ENCODING_STRING,
                    ConfigDef.Importance.LOW, "Encoding of input field before encrypted or decrypted.")
            .define(FIELD_ENCODING_OUT, ConfigDef.Type.STRING, FIELD_ENCODING_STRING,
                    ConfigDef.Importance.LOW, "Encoding of output field after encrypted or decrypted.")
            // Vault
            .define(VAULT_URL, ConfigDef.Type.STRING, ConfigDef.NO_DEFAULT_VALUE,
                    ConfigDef.Importance.HIGH, "URL of the Vault server.")
            .define(VAULT_TOKEN, ConfigDef.Type.PASSWORD, null,
                    ConfigDef.Importance.HIGH, "The token used to access Vault.")
            .define(VAULT_KEY_NAME, ConfigDef.Type.STRING, ConfigDef.NO_DEFAULT_VALUE,
                    ConfigDef.Importance.HIGH, "Name of the key to encrypt or decrypt")
            .define(VAULT_CONTEXT, ConfigDef.Type.STRING, null, Base64StringValidator.singleton,
                    ConfigDef.Importance.MEDIUM, "(optional) Specifies the Base64 context for key derivation. This is required if key derivation is enabled for the key.");

    public abstract Service cryptoService();

    public abstract Set<String> fields();

    public abstract CryptoConfig cryptoCOnfig();

    protected Item.Encoding encodingOf(String value) {
        switch (value) {
            case FIELD_ENCODING_STRING:
                return Item.Encoding.STRING;
            case FIELD_ENCODING_BINARY:
                return Item.Encoding.BINARY;
            case FIELD_ENCODING_BASE64:
                return Item.Encoding.BASE64STRING;
        }
        throw new ConfigException("Invalid encoding: " + value);
    }

    public static class ConfigImpl extends Config {
        private final Service service;
        private final CryptoConfig cryptoConf;
        private final Set<String> fieldSet;

        @Override
        public Service cryptoService() {
            return service;
        }

        @Override
        public Set<String> fields() {
            return fieldSet;
        }

        @Override
        public CryptoConfig cryptoCOnfig() {
            return cryptoConf;
        }

        public ConfigImpl(Map<String, ?> props) {
            final SimpleConfig conf = new SimpleConfig(DEF, props);

            // general configurations
            this.fieldSet = new HashSet<>(conf.getList(FIELDS));
            this.cryptoConf = new CryptoConfig(
                    encodingOf(conf.getString(FIELD_ENCODING_IN)),
                    encodingOf(conf.getString(FIELD_ENCODING_OUT))
            );

            switch (conf.getString(SERVICE)) {
                case SERVICE_VAULT:
                    this.service = vaultService(conf);
                default:
                    throw new ConfigException(SERVICE, conf.getString(SERVICE), "Unknown service");
            }
        }

        private Service vaultService(SimpleConfig conf) {
            final VaultConfig vc = new VaultConfig().address(conf.getString(VAULT_URL));
            if (conf.getString(VAULT_TOKEN) != null) {
                vc.token(conf.getString(VAULT_TOKEN));
            }

            VaultClient client = null;
            try {
                client = new VaultClientImpl(new Vault(vc.build()));
            } catch (VaultException e) {
                throw new ConfigException("Unable to create Vault client: " + e.getMessage());
            }

            VaultCryptoConfig vaultConf = new VaultCryptoConfig(
                    cryptoConf,
                    conf.getString(VAULT_KEY_NAME),
                    Optional.of(conf.getString(VAULT_CONTEXT))
            );

            if (conf.getString(MODE) == MODE_ENCRYPT) {
                return new VaultService.EncryptService(client, vaultConf);
            }
            return new VaultService.DecryptService(client, vaultConf);
        }
    }
}
