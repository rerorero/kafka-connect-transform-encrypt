# kafka-connect-smt-encrypt

[Kafka Connect Transformation](https://kafka.apache.org/documentation/#connect_transforms) (SMT) to encrypt/decrypt fields of records with key management services.

- Encryption and decryption using external key management service. Now it supports:
  - [HashiCopr Vault](https://www.vaultproject.io/docs/secrets/transit)
  - [AWS KMS](https://aws.amazon.com/kms/)
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

#### `condition.field` and `condition.equals` (optional)

Specifies the conditions under which the transformation is be performed or not.

`condition.field` should be JsonPath expression and `condition.equals` should be a string.
When both are set, the transformation is performed only if the value of the field specified by `condition.field` matches the value of `condition.equals`.

All messages are transformed if both are omitted.

## Configurations for HashiCorp Vault

#### `vault.url`

URL of the Vault server.

#### `vault.token`

The Vault token used to access [Vault Transit Engine](https://www.vaultproject.io/api/secret/transit).

#### `vault.key_name`

Specifies the name of the encryption/decryption key to encrypt/decrypt against.

#### `vault.context` (optional)

Specifies the Base64 context for key derivation. This is required if key derivation is enabled.

## Configurations for AWS KMS

#### `awskms.aws_access_key_id`, `awskms.aws_secret_access_key` and `awskms.aws_region`

AWS credentials and the region to access KMS.

#### `awskms.cmk_key_id`

Key ARN of the customer master key (CMK).

#### `awskms.contexts`

Specifies the encryption contexts.
It's parsed as a list of comma delimited `key=value` pairs. e.g. `key1=context1,key2=context2`

#### `awskms.encryption_algorithm`

Specifies [the encryption algorithm](https://aws.github.io/aws-encryption-sdk-java/com/amazonaws/encryptionsdk/CryptoAlgorithm.html).

#### `awskms.endpoint`

Specifies the endpoint to access KMS.

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
