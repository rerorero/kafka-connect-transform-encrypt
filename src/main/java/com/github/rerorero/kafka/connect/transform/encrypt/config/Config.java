package com.github.rerorero.kafka.connect.transform.encrypt.config;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.github.rerorero.kafka.connect.transform.encrypt.condition.Conditions;
import com.github.rerorero.kafka.kms.CryptoConfig;
import com.github.rerorero.kafka.kms.Item;
import com.github.rerorero.kafka.kms.Service;
import com.github.rerorero.kafka.vault.VaultCryptoConfig;
import com.github.rerorero.kafka.vault.VaultService;
import com.github.rerorero.kafka.vault.client.VaultClient;
import com.github.rerorero.kafka.vault.client.VaultClientImpl;
import com.github.rerorero.kafka.jsonpath.JsonPathException;
import com.github.rerorero.kafka.jsonpath.MapSupport;
import com.github.rerorero.kafka.jsonpath.StructSupport;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.transforms.util.SimpleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class Config {
    final static Logger log = LoggerFactory.getLogger(Config.class);

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

    public static final String CONDITION_FIELD = "condition.field";
    public static final String CONDITION_EQUALS = "condition.equals";

    // Vault
    public static final String VAULT_URL = "vault.url";
    public static final String VAULT_TOKEN = "vault.token";
    public static final String VAULT_KEY_NAME = "vault.key_name";
    public static final String VAULT_CONTEXT = "vault.context";

    public static final ConfigDef DEF = new ConfigDef()
            // general
            .define(SERVICE, ConfigDef.Type.STRING, ConfigDef.NO_DEFAULT_VALUE, serviceValidator,
                    ConfigDef.Importance.HIGH, "Name of the service that provides encryption.")
            .define(MODE, ConfigDef.Type.STRING, ConfigDef.NO_DEFAULT_VALUE, modeValidator,
                    ConfigDef.Importance.HIGH, "Specifies the mode, " + MODE_DECRYPT + " or " + MODE_DECRYPT + ".")
            .define(FIELDS, ConfigDef.Type.LIST, new ArrayList<String>(),
                    ConfigDef.Importance.HIGH, "JsonPath expression string to specify the field to be encrypted or decrypted."
                            + "Multiple paths can be specified separated by commas.")
            .define(FIELD_ENCODING_IN, ConfigDef.Type.STRING, FIELD_ENCODING_STRING,
                    ConfigDef.Importance.LOW, "Encoding of input field before encrypted or decrypted.")
            .define(FIELD_ENCODING_OUT, ConfigDef.Type.STRING, null,
                    ConfigDef.Importance.LOW, "Encoding of output field after encrypted or decrypted.")
            .define(CONDITION_FIELD, ConfigDef.Type.STRING, null,
                    ConfigDef.Importance.LOW, "(optional) Specifies the condition for the transform."
                            + "When condition.* are set, transform is performed only if the value of the JsonPath field specified by " + CONDITION_FIELD + " matches " + CONDITION_EQUALS)
            .define(CONDITION_EQUALS, ConfigDef.Type.STRING, null,
                    ConfigDef.Importance.LOW, "(optional) Specifies the condition for the transform."
                            + "When condition.* are set, transform is performed only if the value of the JsonPath field specified by " + CONDITION_FIELD + " matches " + CONDITION_EQUALS)
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

    public abstract FieldSelector fieldSelector();

    public abstract Conditions conditions();

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

    protected static FieldSelector newFieldSelector(Set<String> jsonPaths) {
        FieldSelector fs = new FieldSelector();
        jsonPaths.forEach(path -> {
            try {
                fs.mapGetters.put(path, MapSupport.newGetter(path));
                fs.mapUpdaters.put(path, MapSupport.newUpdater(path));
                fs.structGetters.put(path, StructSupport.newGetter(path));
                fs.structUpdaters.put(path, StructSupport.newUpdater(path));
            } catch (JsonPathException e) {
                throw new ConfigException(FIELDS, path, e.getMessage());
            }
        });
        return fs;
    }

    protected static Conditions newConditions(String field, String comparison) {
        try {
            if (field == null && comparison == null) {
                return new Conditions();
            }
            if (field != null && comparison != null) {
                return new Conditions(field, comparison);
            }
        } catch (JsonPathException e) {
            throw new ConfigException(CONDITION_FIELD, field, e.getMessage());
        }

        throw new ConfigException("You need to specify both " + CONDITION_FIELD + " and " + CONDITION_EQUALS + " to set condition");
    }

    public static class ConfigImpl extends Config {
        private final Service service;
        private final CryptoConfig cryptoConf;
        private final FieldSelector fieldSel;
        private final Conditions conds;

        @Override
        public Service cryptoService() {
            return service;
        }

        @Override
        public FieldSelector fieldSelector() {
            return this.fieldSel;
        }

        @Override
        public Conditions conditions() {
            return this.conds;
        }

        @Override
        public CryptoConfig cryptoCOnfig() {
            return cryptoConf;
        }

        public ConfigImpl(Map<String, ?> props) {
            final SimpleConfig conf = new SimpleConfig(DEF, props);

            // general configurations
            this.fieldSel = newFieldSelector(new HashSet<>(conf.getList(FIELDS)));
            this.conds = newConditions(conf.getString(CONDITION_FIELD), conf.getString(CONDITION_EQUALS));
            this.cryptoConf = new CryptoConfig(
                    encodingOf(conf.getString(FIELD_ENCODING_IN)),
                    encodingOf(conf.getString(FIELD_ENCODING_OUT) != null ? conf.getString(FIELD_ENCODING_OUT) : conf.getString(FIELD_ENCODING_IN))
            );


            if (conf.getString(SERVICE).equals(SERVICE_VAULT)) {
                this.service = vaultService(conf);
            } else {
                throw new ConfigException(SERVICE, conf.getString(SERVICE), "unknown service");
            }
        }

        private Service vaultService(SimpleConfig conf) {
            final VaultConfig vc = new VaultConfig().address(conf.getString(VAULT_URL));
            if (conf.getPassword(VAULT_TOKEN) != null) {
                vc.token(conf.getPassword(VAULT_TOKEN).value());
            }

            VaultClient client = null;
            try {
                client = new VaultClientImpl(new Vault(vc.build(), 1));
            } catch (VaultException e) {
                throw new ConfigException("Unable to create Vault client: " + e.getMessage());
            }

            VaultCryptoConfig vaultConf = new VaultCryptoConfig(
                    cryptoConf,
                    conf.getString(VAULT_KEY_NAME),
                    Optional.ofNullable(conf.getString(VAULT_CONTEXT))
            );

            if (conf.getString(MODE).equals(MODE_ENCRYPT)) {
                return new VaultService.EncryptService(client, vaultConf);
            }
            return new VaultService.DecryptService(client, vaultConf);
        }
    }
}
