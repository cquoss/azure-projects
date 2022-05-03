package de.quoss.azure.adb2c;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IClientCredential;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionPage;
import com.microsoft.graph.requests.UserCollectionRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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
            new Main().run(args);
        } catch (AzureAdB2cException e) {
            LOGGER.error("", e);
            System.exit(1);
        }
    }

    private void run(final String[] args) throws AzureAdB2cException {
        final String methodName = "run()";
        final GraphServiceClient client = createClient();
        if (args.length == 0) {
            throw new AzureAdB2cException("USAGE: java " + getClass().getCanonicalName() + " command ...");
        }
        switch (args[0]) {
            case "listUsers" :
                listUsers(client);
                break;
            default:
                throw new AzureAdB2cException("Unknown command: " + args[0]);
        }
    }

    private void listUsers(final GraphServiceClient client) {
        UserCollectionPage page = client.users().buildRequest()
                .select("id,userPrincipalName")
                .get();
        while (page != null) {
            List<User> users = page.getCurrentPage();
            for (User user : users) {
                logUser(user);
            }
            final UserCollectionRequestBuilder nextPage = page.getNextPage();
            if (nextPage == null) {
                break;
            } else {
                page = nextPage.buildRequest()
                        .get();
            }
        }
    }

    private void logUser(final User user) {
        LOGGER.info("User [id={},userPrincipalName={}]", user.id, user.userPrincipalName);
    }

    /**
     * Create graph service client.
     * @return Client.
     * @throws AzureAdB2cException Thrown in case of error.
     */
    private GraphServiceClient createClient() throws AzureAdB2cException {
        final String tenantId = getProperty("tenant-id");
        final String clientId = getProperty("client-id");
        final String scope = getProperty("scope");
        final String secret = getProperty("secret");
        final ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(secret)
                .tenantId(tenantId)
                .build();
        final List<String> scopes = new LinkedList<>();
        scopes.add(scope);
        final TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(scopes, credential);
        return GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();
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
