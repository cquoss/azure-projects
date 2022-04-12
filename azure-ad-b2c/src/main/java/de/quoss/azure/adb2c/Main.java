package de.quoss.azure.adb2c;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IClientCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Properties;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private static final Properties PROPERTIES = new Properties();

    static void loadProperties() throws AzureAdB2cException {
        final Class<Main> c = Main.class;
        final String resourceName = c.getName() + ".properties";
        InputStream stream = c.getClassLoader().getResourceAsStream(resourceName);
        if (stream == null) {
            throw new AzureAdB2cException("Classpath resource " + resourceName + " not available.");
        }
        try {
            PROPERTIES.load(stream);
        } catch (IOException e) {
            throw new AzureAdB2cException(e);
        }
    }

    public static void main(final String[] args) {
        try {
            loadProperties();
            new Main().run();
        } catch (AzureAdB2cException e) {
            LOGGER.error("", e);
            System.exit(1);
        }
    }

    private void run() throws AzureAdB2cException {
        final String methodName = "run()";
        String token = authenticateApplication();
        LOGGER.info("{} [token={}]", methodName, token);
    }

    /**
     * Look here: https://github.com/AzureAD/microsoft-authentication-library-for-java/blob/dev/src/samples/confidential-client/ClientCredentialGrant.java
     *
     * @return Access token as String.
     * @throws AzureAdB2cException Thrown in case of error.
     */
    private String authenticateApplication() throws AzureAdB2cException {
        final String authority = getProperty("authority");
        final String clientId = getProperty("client-id");
        final String scope = getProperty("scope");
        final String secret = getProperty("secret");
        IClientCredential credential = ClientCredentialFactory.createFromSecret(secret);
        ConfidentialClientApplication app;
        try {
            app = ConfidentialClientApplication.builder(clientId, credential)
                    .authority(authority)
                    .build();
        } catch (MalformedURLException e) {
            throw new AzureAdB2cException(e);
        }
        ClientCredentialParameters parameters = ClientCredentialParameters.builder(Collections.singleton(scope)).build();
        IAuthenticationResult result = app.acquireToken(parameters).join();
        return result.accessToken();
    }

    private String getProperty(final String key) throws AzureAdB2cException {
        return getProperty(key, null);
    }

    private String getProperty(final String key, final String defaultValue) throws AzureAdB2cException {
        final String fullKey = getClass().getName() + "." + key;
        String result = System.getProperty(fullKey);
        if (result == null) {
            result = PROPERTIES.getProperty(fullKey);
        }
        if (result == null) {
            result = defaultValue;
        }
        if (result == null) {
            throw new AzureAdB2cException("Mandatory property " + fullKey + " not set.");
        }
        return result;
    }

}
