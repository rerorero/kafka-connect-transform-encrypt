package com.github.rerorero.kafka.connect.transform.encrypt.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.github.rerorero.kafka.aws.AWSKMSCryptoConfig;
import com.github.rerorero.kafka.aws.AWSKeyManagementService;
import com.github.rerorero.kafka.connect.transform.encrypt.condition.Conditions;
import com.github.rerorero.kafka.gcp.GCPKMSCryptoConfig;
import com.github.rerorero.kafka.gcp.GCPKeyManagementService;
import com.github.rerorero.kafka.jsonpath.JsonPathException;
import com.github.rerorero.kafka.jsonpath.MapSupport;
import com.github.rerorero.kafka.jsonpath.StructSupport;
import com.github.rerorero.kafka.kms.CryptoConfig;
import com.github.rerorero.kafka.kms.Item;
import com.github.rerorero.kafka.kms.Service;
import com.github.rerorero.kafka.vault.VaultCryptoConfig;
import com.github.rerorero.kafka.vault.VaultService;
import com.github.rerorero.kafka.vault.client.VaultClient;
import com.github.rerorero.kafka.vault.client.VaultClientImpl;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.connect.transforms.util.SimpleConfig;
import org.bouncycastle.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class Config {
    final static Logger log = LoggerFactory.getLogger(Config.class);

    // general configurations
    public static final String SERVICE = "service";
    public static final String SERVICE_VAULT = "vault";
    public static final String SERVICE_AWSKMS = "awskms";
    public static final String SERVICE_GCPKMS = "gcpkms";
    private static final OneOfValidator<String> serviceValidator = new OneOfValidator<>(SERVICE_VAULT, SERVICE_AWSKMS, SERVICE_GCPKMS);

    public static final String MODE = "mode";
    public static final String MODE_ENCRYPT = "encrypt";
    public static final String MODE_DECRYPT = "decrypt";
    private static final OneOfValidator<String> modeValidator = new OneOfValidator<>(MODE_ENCRYPT, MODE_DECRYPT);

    public static final String FIELDS = "fields";
    public static final String FIELD_ENCODING_OUT = "field.encoding.out";
    public static final String FIELD_ENCODING_STRING = "string";
    public static final String FIELD_ENCODING_BINARY = "binary";
    private static final OneOfValidator<String> encodingValidator = new OneOfValidator<>(FIELD_ENCODING_STRING, FIELD_ENCODING_BINARY);

    public static final String CONDITION_FIELD = "condition.field";
    public static final String CONDITION_EQUALS = "condition.equals";

    // Vault
    public static final String VAULT_URL = "vault.url";
    public static final String VAULT_TOKEN = "vault.token";
    public static final String VAULT_KEY_NAME = "vault.key_name";
    public static final String VAULT_CONTEXT = "vault.context";

    // AWS KMS
    public static final String AWSKMS_ACCESS_KEY_ID = "awskms.aws_access_key_id";
    public static final String AWSKMS_SECRET_ACCESS_KEY = "awskms.aws_secret_access_key";
    public static final String AWSKMS_REGION = "awskms.aws_region";
    public static final String AWSKMS_CMK_KEYID = "awskms.cmk_key_id";
    public static final String AWSKMS_CONTEXTS = "awskms.contexts";
    public static final String AWSKMS_ENCRYPTION_ALGORITHM = "awskms.encryption_algorithm";
    public static final String AWSKMS_ENDPOINT = "awskms.endpoint";

    // GCP Cloud KMS
    public static final String GCPKMS_KEY_PROJECT_ID = "gcpkms.key.project_id";
    public static final String GCPKMS_KEY_LOCATION_ID = "gcpkms.key.location_id";
    public static final String GCPKMS_KEY_RING_ID = "gcpkms.key.ring_id";
    public static final String GCPKMS_KEY_KEY_ID = "gcpkms.key.key_id";

    public static final ConfigDef DEF = new ConfigDef()
            // general
            .define(SERVICE, ConfigDef.Type.STRING, ConfigDef.NO_DEFAULT_VALUE, serviceValidator,
                    ConfigDef.Importance.HIGH, "Name of the service that provides encryption.")
            .define(MODE, ConfigDef.Type.STRING, ConfigDef.NO_DEFAULT_VALUE, modeValidator,
                    ConfigDef.Importance.HIGH, "Specifies the mode, " + MODE_DECRYPT + " or " + MODE_DECRYPT + ".")
            .define(FIELDS, ConfigDef.Type.LIST, new ArrayList<String>(),
                    ConfigDef.Importance.HIGH, "JsonPath expression string to specify the field to be encrypted or decrypted."
                            + "Multiple paths can be specified separated by commas.")
            .define(FIELD_ENCODING_OUT, ConfigDef.Type.STRING, FIELD_ENCODING_STRING, encodingValidator,
                    ConfigDef.Importance.LOW, "Encoding of output field after encrypted or decrypted.")
            .define(CONDITION_FIELD, ConfigDef.Type.STRING, null,
                    ConfigDef.Importance.LOW, "(optional) Specifies the condition for the transform."
                            + "When condition.* are set, transform is performed only if the value of the JsonPath field specified by " + CONDITION_FIELD + " matches " + CONDITION_EQUALS)
            .define(CONDITION_EQUALS, ConfigDef.Type.STRING, null,
                    ConfigDef.Importance.LOW, "(optional) Specifies the condition for the transform."
                            + "When condition.* are set, transform is performed only if the value of the JsonPath field specified by " + CONDITION_FIELD + " matches " + CONDITION_EQUALS)
            // Vault
            .define(VAULT_URL, ConfigDef.Type.STRING, null,
                    ConfigDef.Importance.HIGH, "URL of the Vault server.")
            .define(VAULT_TOKEN, ConfigDef.Type.PASSWORD, null,
                    ConfigDef.Importance.HIGH, "The token used to access Vault.")
            .define(VAULT_KEY_NAME, ConfigDef.Type.STRING, null,
                    ConfigDef.Importance.HIGH, "Name of the key to encrypt or decrypt")
            .define(VAULT_CONTEXT, ConfigDef.Type.STRING, null, Base64StringValidator.singleton,
                    ConfigDef.Importance.MEDIUM, "(optional) Specifies the Base64 context for key derivation. This is required if key derivation is enabled for the key.")
            // AWS KMS
            .define(AWSKMS_ACCESS_KEY_ID, ConfigDef.Type.PASSWORD, null,
                    ConfigDef.Importance.MEDIUM, "AWS_ACCESS_KEY_ID of the AWS credentials to access KMS")
            .define(AWSKMS_SECRET_ACCESS_KEY, ConfigDef.Type.PASSWORD, null,
                    ConfigDef.Importance.MEDIUM, "AWS_SECRET_ACCESS_KEY of the AWS credentials to access KMS")
            .define(AWSKMS_REGION, ConfigDef.Type.STRING, null,
                    ConfigDef.Importance.MEDIUM, "The AWS region to use.")
            .define(AWSKMS_CMK_KEYID, ConfigDef.Type.STRING, null,
                    ConfigDef.Importance.HIGH, "Key ARN of your AWS KMS customer master key (CMK)")
            .define(AWSKMS_CONTEXTS, ConfigDef.Type.STRING, "",
                    ConfigDef.Importance.MEDIUM, "Specifies the encryption contexts with 'key=value' pairs separated by commas.")
            .define(AWSKMS_ENCRYPTION_ALGORITHM, ConfigDef.Type.STRING, null,
                    ConfigDef.Importance.LOW, "The encryption algorithm.")
            .define(AWSKMS_ENDPOINT, ConfigDef.Type.STRING, null,
                    ConfigDef.Importance.LOW, "(optional) Overrides the URL of the default KMS endpoint with given URL.")
            // GCP Cloud KMS
            .define(GCPKMS_KEY_PROJECT_ID, ConfigDef.Type.STRING, null,
                    ConfigDef.Importance.HIGH, "GCP project ID for the key")
            .define(GCPKMS_KEY_LOCATION_ID, ConfigDef.Type.STRING, null,
                    ConfigDef.Importance.HIGH, "Location of the keyring")
            .define(GCPKMS_KEY_RING_ID, ConfigDef.Type.STRING, null,
                    ConfigDef.Importance.HIGH, "Keyring of the key")
            .define(GCPKMS_KEY_KEY_ID, ConfigDef.Type.STRING, null,
                    ConfigDef.Importance.HIGH, "The key to use for encryption");

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

    protected void ensureStringValueExists(SimpleConfig conf, String key, String message) {
        if (conf.getString(key) == null) {
            throw new ConfigException(key, null, message);
        }
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
            this.cryptoConf = new CryptoConfig(encodingOf(conf.getString(FIELD_ENCODING_OUT)));

            if (conf.getString(SERVICE).equals(SERVICE_VAULT)) {
                this.service = vaultService(conf);
            } else if (conf.getString(SERVICE).equals(SERVICE_AWSKMS)) {
                this.service = awsKmsService(conf);
            } else if (conf.getString(SERVICE).equals(SERVICE_GCPKMS)) {
                this.service = gcpKmsService(conf);
            } else {
                throw new ConfigException(SERVICE, conf.getString(SERVICE), "unknown service");
            }
        }

        private Service vaultService(SimpleConfig conf) {
            ensureStringValueExists(conf, VAULT_URL, "Required parameter for " + SERVICE_VAULT + " service");
            ensureStringValueExists(conf, VAULT_KEY_NAME, "Required parameter for " + SERVICE_VAULT + " service");

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
                    conf.getString(VAULT_KEY_NAME),
                    Optional.ofNullable(conf.getString(VAULT_CONTEXT))
            );

            if (conf.getString(MODE).equals(MODE_ENCRYPT)) {
                return new VaultService.EncryptService(client, vaultConf);
            }
            return new VaultService.DecryptService(client, vaultConf);
        }

        private Service awsKmsService(SimpleConfig conf) {
            ensureStringValueExists(conf, AWSKMS_CMK_KEYID, "Required parameter for " + SERVICE_AWSKMS + " service");

            if (conf.getString(AWSKMS_ENDPOINT) != null && conf.getString(AWSKMS_REGION) == null) {
                throw new ConfigException(AWSKMS_REGION, null, "Required parameter if " + AWSKMS_ENDPOINT + " is specified");
            }

            String accessKeyID = conf.getPassword(AWSKMS_ACCESS_KEY_ID) == null ? null : conf.getPassword(AWSKMS_ACCESS_KEY_ID).value();
            String accessSecret = conf.getPassword(AWSKMS_SECRET_ACCESS_KEY) == null ? null : conf.getPassword(AWSKMS_SECRET_ACCESS_KEY).value();
            Optional<AWSCredentials> creds;
            if (accessKeyID != null && accessSecret != null) {
                creds = Optional.of(new BasicAWSCredentials(accessKeyID, accessSecret));
            } else if (accessKeyID == null && accessSecret == null) {
                creds = Optional.empty();
            } else {
                throw new ConfigException("Both " + AWSKMS_ACCESS_KEY_ID + " and " + AWSKMS_SECRET_ACCESS_KEY + " must be specified");
            }

            Map<String, String> context = new HashMap<>();
            for (String pair : Strings.split(conf.getString(AWSKMS_CONTEXTS), ',')) {
                if (pair.equals("")) {
                    break;
                }
                String[] keyAndValue = pair.split(pair, '=');
                if (keyAndValue.length != 2) {
                    throw new ConfigException(AWSKMS_CONTEXTS, pair, "Use the 'key=value' format, separated by commas.");
                }
            }

            AWSKMSCryptoConfig config = new AWSKMSCryptoConfig(creds, Optional.ofNullable(conf.getString(AWSKMS_REGION)), conf.getString(AWSKMS_CMK_KEYID),
                    context, Optional.ofNullable(conf.getString(AWSKMS_ENCRYPTION_ALGORITHM)), Optional.ofNullable(conf.getString(AWSKMS_ENDPOINT)));

            if (conf.getString(MODE).equals(MODE_ENCRYPT)) {
                return new AWSKeyManagementService.EncryptService(config);
            }
            return new AWSKeyManagementService.DecryptService(config);
        }

        private Service gcpKmsService(SimpleConfig conf) {
            ensureStringValueExists(conf, GCPKMS_KEY_PROJECT_ID, "Required parameter for " + SERVICE_GCPKMS + " service");
            ensureStringValueExists(conf, GCPKMS_KEY_LOCATION_ID, "Required parameter for " + SERVICE_GCPKMS + " service");
            ensureStringValueExists(conf, GCPKMS_KEY_RING_ID, "Required parameter for " + SERVICE_GCPKMS + " service");
            ensureStringValueExists(conf, GCPKMS_KEY_KEY_ID, "Required parameter for " + SERVICE_GCPKMS + " service");

            final GCPKMSCryptoConfig cryptoConfig = new GCPKMSCryptoConfig(
                    conf.getString(GCPKMS_KEY_PROJECT_ID),
                    conf.getString(GCPKMS_KEY_LOCATION_ID),
                    conf.getString(GCPKMS_KEY_RING_ID),
                    conf.getString(GCPKMS_KEY_KEY_ID)
            );

            if (conf.getString(MODE).equals(MODE_ENCRYPT)) {
                return new GCPKeyManagementService.EncryptService(cryptoConfig);
            }
            return new GCPKeyManagementService.DecryptService(cryptoConfig);
        }
    }
}
