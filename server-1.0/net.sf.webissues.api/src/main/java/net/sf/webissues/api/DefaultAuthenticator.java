package net.sf.webissues.api;

import java.net.URL;

/**
 * Simple {@link Authenticator} implementation that stores the username and
 * password and provides the client with these when invoked.
 * 
 * @see Client#setAuthenticator(Authenticator)
 */
public class DefaultAuthenticator implements Authenticator {
    private String username;
    private char[] password;

    public DefaultAuthenticator(String username, char[] password) {
        super();
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    /**
     * Set the username.
     * 
     * @param username username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    public char[] getPassword() {
        return password;
    }

    /**
     * Set the password.
     * 
     * @param password password
     */
    public void setPassword(char[] password) {
        this.password = password;
    }

    public Credentials getCredentials(URL url) {
        return new Authenticator.Credentials() {

            public String getUsername() {
                return username;
            }

            public char[] getPassword() {
                return password;
            }
        };
    }
}
