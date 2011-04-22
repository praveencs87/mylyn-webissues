package org.webissues.api;

import java.io.Serializable;
import java.net.URL;

/**
 * Call-back to retrieve authentication details when required.
 */
public interface Authenticator extends Serializable {

    /**
     * Get the authentication credentials for the provided server URL.
     * <code>null</code> should be returned if authentication is cancelled or
     * cannot be retrieved for any other reason.
     * 
     * @param url server URL
     * @return credentials
     */
    Credentials getCredentials(URL url);

    /**
     * Provides the user name and password.
     */
    public interface Credentials {
        /**
         * Get the user name
         * 
         * @return user name
         */
        String getUsername();

        /**
         * Get the password.
         * 
         * @return password
         */
        char[] getPassword();
    }
}
