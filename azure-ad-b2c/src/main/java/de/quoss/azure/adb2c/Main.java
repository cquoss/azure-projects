package de.quoss.azure.adb2c;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionPage;
import com.microsoft.graph.requests.UserCollectionRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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

    private List<User> getUser(final String token) throws AzureAdB2cException {
        final String methodName = "getUser(String)";
        String url = getProperty("graph-base-url") + "/users";
        HttpClient client = HttpClient.newBuilder()
                .proxy(ProxySelector.of(new InetSocketAddress(8080)))
                .build();
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .header("Authorization", "Bearer " + token)
                    .build();
        } catch (URISyntaxException e) {
            throw new AzureAdB2cException(e);
        }
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException | IOException e) {
            Thread.currentThread().interrupt();
            throw new AzureAdB2cException(e);
        }
        final int statusCode = response.statusCode();
        if (statusCode == 200) {
            try {
                return new ObjectMapper().readValue(response.body(), new TypeReference<List>() {
                });
            } catch (JsonProcessingException e) {
                throw new AzureAdB2cException(e);
            }
        } else {
            String body = response.body();
            LOGGER.error("{} [statusCode={},body={}]", methodName, statusCode, body);
            return new LinkedList<>();
        }
    }

}
