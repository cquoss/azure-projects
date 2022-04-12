package de.quoss.azure.adb2c;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IClientCredential;

import java.net.MalformedURLException;
import java.util.Collections;

public class Main {

    public static void main(final String[] args) {
        try {
            new Main().run();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void run() throws MalformedURLException {
        String token = authenticateApplication();
        System.out.format("token: %s%n", token);
    }

    /**
     * Look here: https://github.com/AzureAD/microsoft-authentication-library-for-java/blob/dev/src/samples/confidential-client/ClientCredentialGrant.java
     *
     * @return Access token as String.
     * @throws MalformedURLException Thrown in case of error.
     */
    private String authenticateApplication() throws MalformedURLException {
        IClientCredential credential = ClientCredentialFactory.createFromSecret("get-secret-from-keepass");
        ConfidentialClientApplication app = ConfidentialClientApplication.builder("get-public-client-id-from-keepass", credential)
                .authority("get-authority-from-keepass")
                .build();
        ClientCredentialParameters parameters = ClientCredentialParameters.builder(Collections.singleton("get-scope-from-keepass")).build();
        IAuthenticationResult result = app.acquireToken(parameters).join();
        return result.accessToken();
    }

}
