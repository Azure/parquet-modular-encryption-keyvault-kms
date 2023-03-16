### What's in this repository?

Inside this repository you will find the following artifacts:

* Sample Java Library that implements an encryption and decryption class that leverages Azure Key Vault and Azure Managed HSM, and relevant pom file for compiling the source code ([./src/main/java/com/microsoft/solutions/keyvaultcrypto](./src/main/java/com/microsoft/solutions/keyvaultcrypto))
* Sample Notebook that will attempt to register the UDF and do some quick encryption and decryption ([./notebook/ParquetKMSSample.ipynb](./notebook/ParquetKMSSample.ipynb))

### Problem Statement

Client-side encryption in Azure is usually left to application developers to come up with methods to decrypt data on demand. While at-rest options for encryption of data stored in your Azure Storage accounts exist (https://learn.microsoft.com/en-us/azure/storage/common/storage-service-encryption), client side encryption remains difficult to develop and implement at scale, especially for large, partitioned datasets stored in Parquet format.

This code repository was created to show a solution that uses Azure-native solutions for implementing Parquet modular encryption using Azure Key Vault for a Key Management Service (KMS).

### Solution Architecture

Column-level encryption is a feature supported by the Parquet specification (https://github.com/apache/parquet-format/blob/master/Encryption.md), and it requires configuring your Spark and/or Hadoop environment with specific settings, and implementing a Java class that overrides the base classes required (https://spark.apache.org/docs/latest/sql-data-sources-parquet.html#columnar-encryption) for the wrapping and unwrapping of keys. To leverage Azure Key Vault or Azure Managed HSM as a solution, methods should also exist in your class for authenticating to your chosen Azure KMS. You can accomplish both tasks using the various Azure SDKS:

* Azure Identity Libraries for Java: https://learn.microsoft.com/en-us/java/api/overview/azure/identity?view=azure-java-stable
* Azure Key Vault Libraries for Java: https://learn.microsoft.com/en-us/java/api/overview/azure/key-vault?view=azure-java-stable

### Important: Before you begin

This sample code and contained classes are designed to give you a starting point of how you would accomplish these cryptographic operations using Spark. Note that these are just starting points; this code is not designed to be run in production. Things like proper exception handling and tests would still need to be provided.

### Core Components

* A Managed Spark Environment, such as:
  * Azure Databricks
  * Azure Synapse Spark Pools
* An Azure Cryptographic Tool, either:
  * Azure Key Vault
  * Azure Managed HSM
* Network Connectivity between your Spark compute engine and the chosen Cryptographic tool
* An Azure Service Principal

## Deploying This Solution

### Creating a Service Principal and Assign Permissions

This solution relies on the ```DefaultAzureCredentialBuilder``` class of the Azure Identity Java SDK (https://learn.microsoft.com/en-us/java/api/com.azure.identity.defaultazurecredentialbuilder?view=azure-java-stable) to authenticate to Azure Key Vault / Azure Managed HSM resources for cryptographic operations, specifically the use of environment variables (https://learn.microsoft.com/en-us/java/api/com.azure.identity.environmentcredential?view=azure-java-stable). Before getting started you should create a service principal and a client secret. Note these values down after you create them. More details can be found here: https://learn.microsoft.com/en-us/azure/active-directory/develop/howto-create-service-principal-portal

Once you have your service principal created, you will need to apply an access policy or RBAC role for the principal. The required operations of this example that you should provide are:

* Get Key
* List Keys
* Encrypt
* Decrypt

For Managed HSM, you should give the principal the following role:

* Managed HSM Crypto User

### Compiling the JAR

The included ```src``` folder and .pom file should have everything you need to get started with this example. This sample solution takes dependencies on the ```com.azure.identity.*``` and ```com.azure.security.keyvault.keys.cryptography.*``` libraries.

### Adding the JAR/Classes to your Classpath

Once compiled, you will need to provide the JAR to your Java classpath. This will vary depending on where you are planning to use Spark. For more detailed information about making your JARs available in various Azure managed Spark environments, please refer to the following documentation:

* Azure Databricks - Add Workspace Library: https://learn.microsoft.com/en-us/azure/databricks/libraries/workspace-libraries
* Azure Synapse - Manage libraries for Apache Spark in Azure Synapse Analytics: https://learn.microsoft.com/en-us/azure/synapse-analytics/spark/apache-spark-azure-portal-add-libraries

### Configure your Spark Environment Variables

Next, you'll need to add your environment variables for your service principal. This will vary depending on your platform. In the end, the following environment variables need to be set:

* AZURE_TENANT_ID (Your Azure Tenant ID)
* AZURE_CLIENT_ID (Your Service Principal Client Id)
* AZURE_CLIENT_SECRET (The secret your created when you created your service principal)

To set these variables, please refer to your managed Spark platform in Azure:

* Azure Databricks: https://learn.microsoft.com/en-us/azure/databricks/kb/clusters/validate-environment-variable-behavior
* Azure Synapse Environment Variables: https://learn.microsoft.com/en-us/azure/synapse-analytics/spark/apache-spark-azure-create-spark-configuration

If you are running locally or remotely, you can set this as you set any local system environment variables.

### Setting Hadoop / Spark Settings for Encryption

Next, set your required Hadoop and Spark settings for your cluster. You will need to set the following options:

* spark.hadoop.parquet.crypto.factory.class org.apache.parquet.crypto.keytools.PropertiesDrivenCryptoFactory
* spark.hadoop.parquet.encryption.kms.instance.url https://yourvaultname.vault.azure.net
* spark.hadoop.parquet.encryption.kms.client.class com.microsoft.solutions.kmsclient.KeyVaultClient

### Encrypting your Columns and Footers

To encrypt your columns in your parquet file, on write you need to provide a couple extra options to the write command. For example, if you have a Spark dataframe named "df" that contains a column "CustomerID" and we want to encrypt it, you need to first know which key we want to use to wrap the encryption key. We need to know the key name and key version (keyname/keyversion). You should also choose a key for your footer.

Our write command in spark will look something like this:

```python
utput_storage_account_name = "your storage account name"
output_storage_container_name = "your storage container names"

df.write.mode("overwrite").option("parquet.encryption.footer.key","<key name>/<key version>").option("parquet.encryption.column.keys","<key name>/<key version>:CustomerID").format("parquet").save("abfss://{0}@{1}.dfs.core.windows.net/encryptionDemo".format(output_storage_container_name,output_storage_account_name))
```

### Decrypting Data

If your Spark clusters are configured with the required JARs, Spark configuration, and environment variables, decryption on read is handled automatically. Without one or more of those requirements, you will receive an "No Keys Available" exception.

## Additional Considerations

* This method does not apply to Delta format.

## Contributing

This project welcomes contributions and suggestions.  Most contributions require you to agree to a
Contributor License Agreement (CLA) declaring that you have the right to, and actually do, grant us
the rights to use your contribution. For details, visit https://cla.opensource.microsoft.com.

When you submit a pull request, a CLA bot will automatically determine whether you need to provide
a CLA and decorate the PR appropriately (e.g., status check, comment). Simply follow the instructions
provided by the bot. You will only need to do this once across all repos using our CLA.

This project has adopted the [Microsoft Open Source Code of Conduct](https://opensource.microsoft.com/codeofconduct/).
For more information see the [Code of Conduct FAQ](https://opensource.microsoft.com/codeofconduct/faq/) or
contact [opencode@microsoft.com](mailto:opencode@microsoft.com) with any additional questions or comments.

## Trademarks

This project may contain trademarks or logos for projects, products, or services. Authorized use of Microsoft 
trademarks or logos is subject to and must follow 
[Microsoft's Trademark & Brand Guidelines](https://www.microsoft.com/en-us/legal/intellectualproperty/trademarks/usage/general).
Use of Microsoft trademarks or logos in modified versions of this project must not cause confusion or imply Microsoft sponsorship.
Any use of third-party trademarks or logos are subject to those third-party's policies.
