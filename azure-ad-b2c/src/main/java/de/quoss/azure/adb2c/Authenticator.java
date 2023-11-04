package de.quoss.azure.adb2c;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IClientCredential;

import java.net.MalformedURLException;
import java.util.Collections;

public class Authenticator {

    /**
     * Look here: https://github.com/AzureAD/microsoft-authentication-library-for-java/blob/dev/src/samples/confidential-client/ClientCredentialGrant.java
     *
     * @return Access token as String.
     * @throws AzureAdB2cException Thrown in case of error.
     */

    public String getToken(final String authority, final String clientId, final String scope, final String secret)
            throws AzureAdB2cException {
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

}
