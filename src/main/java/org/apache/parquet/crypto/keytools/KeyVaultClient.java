package org.apache.parquet.crypto.keytools;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.identity.DefaultAzureCredential;

import com.azure.core.exception.ResourceNotFoundException;
import com.azure.security.keyvault.keys.KeyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClient;
import com.azure.security.keyvault.keys.cryptography.CryptographyClientBuilder;
import com.azure.security.keyvault.keys.cryptography.models.DecryptResult;
import com.azure.security.keyvault.keys.cryptography.models.EncryptionAlgorithm;

import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.crypto.KeyAccessDeniedException;
import org.apache.parquet.crypto.keytools.KmsClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class KeyVaultClient implements KmsClient 
{
    private String kmsToken;
    private Configuration hadoopConfiguration;
    private String keyVaultUri;
    private static final EncryptionAlgorithm algorithm = EncryptionAlgorithm.fromString("RSA1_5");
    private DefaultAzureCredential azureCredential = null;

    KeyClient keyClient = null;

    @Override
    public void initialize( Configuration configuration, String kmsInstanceID, String kmsInstanceURL, String accessToken ) throws KeyAccessDeniedException {
        keyVaultUri = kmsInstanceURL;
        kmsToken = accessToken;
        hadoopConfiguration = configuration;
    }    

    @Override
    public String wrapKey(byte[] keyBytes, String masterKeyIdentifier) throws KeyAccessDeniedException {
        String result = null;
        String keyUri = keyVaultUri + "/keys/" + masterKeyIdentifier.toString();

        DefaultAzureCredential azureCredential = new DefaultAzureCredentialBuilder().build();

        System.out.println("Retreiving key: " + keyUri);
       
        try {
            CryptographyClient cryptoClient = new CryptographyClientBuilder()
                .keyIdentifier(keyUri)
                .credential(azureCredential)
                .buildClient();
            byte[] cipherText = cryptoClient.encrypt(algorithm, keyBytes).getCipherText();
            byte[] base64Value = Base64.getEncoder().encode(cipherText);
            result = new String(base64Value, StandardCharsets.UTF_8);
        } catch (ResourceNotFoundException rnf) {
            throw new KeyAccessDeniedException(rnf.getMessage());
        }
        return result;
    }

    @Override
    public byte[] unwrapKey(String wrappedKey, String masterKeyIdentifier) throws KeyAccessDeniedException {
        byte[] plaintext = null;
        String keyUri = keyVaultUri + "/keys/" + masterKeyIdentifier.toString();
        System.out.println("Retreiving key: " + keyUri);
        
        try {
            DefaultAzureCredential azureCredential = new DefaultAzureCredentialBuilder().build();            
            CryptographyClient cryptoClient = new CryptographyClientBuilder()
                .keyIdentifier(keyUri)
                .credential(azureCredential)
                .buildClient();
            byte[] cipherText = Base64.getDecoder().decode(wrappedKey.getBytes(StandardCharsets.UTF_8));
            DecryptResult decryptedResult = cryptoClient.decrypt(algorithm, cipherText);
            plaintext = decryptedResult.getPlainText();
        } catch (ResourceNotFoundException rnf) {
            throw new KeyAccessDeniedException(rnf.getMessage());
        }
        return plaintext;
    }
}