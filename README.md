# kafka-connect-smt-encrypt

[Kafka Connect Transformation](https://kafka.apache.org/documentation/#connect_transforms) (SMT) to encrypt/decrypt fields of records with key management services.

- Encryption and decryption using external key management service. Now it supports:
  - [HashiCopr Vault](https://www.vaultproject.io/docs/secrets/transit)
- Encryption and decryption at the field level.
- You can use [JsonPath](https://github.com/json-path/JsonPath) to specify the fields. NOTE: It has limited support for JsonPath syntax for now, please see [JsonPath Limitations](#jsonpath-limitations).
- Parse as a Struct when schema present, or a Map in the case of schemaless data.

# Installation

Download the jar file from the [release page](https://github.com/rerorero/kafka-connect-transform-encrypt/releases) and copy it into a directory that is under one of the `plugin.path`.
[This doccument](https://docs.confluent.io/platform/current/connect/transforms/custom.html) would help you.

# Configurations

You can see the example configuration file [here](./e2e/connect_config.json).

#### `type`

Specifies the type designed for the record key or value:

- `com.github.rerorero.kafka.connect.transform.encrypt.Transform$Key`
- `com.github.rerorero.kafka.connect.transform.encrypt.Transform$Value`

#### `service`

Defines the key management service to encrypt/decrypt. Valid values are:

- `vault`

#### `mode`

Specifies the mode. Valid values are:

- `encrypt`
- `decrypt`

#### `fields`

JsonPath expression strings to specify the field to be encrypted or decrypted. Multiple path can be specified separated by commas.

NOTE: It has limited support for JsonPath syntax for now, please see [JsonPath Limitations](#jsonpath-limitations).

## Configurations for HashiCorp Vault

#### `vault.url`

URL of the Vault server.

#### `vault.token`

The Vault token used to access [Vault Transit Engine](https://www.vaultproject.io/api/secret/transit).

#### `vault.key_name`

Specifies the name of the encryption/decryption key to encrypt/decrypt against.

#### `vault.context` (optional)

Specifies the Base64 context for key derivation. This is required if key derivation is enabled.

## JsonPath Limitations

Only the following syntaxes are supported for now:

| Operator     | Description                                                                 |
| ------------ | --------------------------------------------------------------------------- |
| `$`          | The root element. All JsonPath string has to be started with this operator. |
| `*`          | Wildcard. Only supported for use as an array index.                         |
| `.<name>`    | Dot-notated child.                                                          |
| `['name']`   | Bracket-notated child. Multiple names are not supported.                    |
| `[<number>]` | Array index. Multiple indices are not supported.                            |

# Build and Deployment

Build and test:

```
gradlew build test
```

Run integration test:

```
gradlew build shadowJar
cd e2e
./test.sh
echo $? # should exit with 0
```
