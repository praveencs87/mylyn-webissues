package org.webissues.api;

/**
 * Callback interface used when the user must change their password on
 * login.
 */
public interface PasswordChangeCallback {
    /**
     * Get the new password to use. Return <code>null</code> to cancel the
     * password change and abort the login.
     * 
     * @return new password
     */
    char[] getNewPassword();
}