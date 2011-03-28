package net.sf.webissues.api;

/**
 * Type of access
 */
public enum Access {
    /**
     * Access not allowed
     */
    NONE,
    /**
     * Normal access
     */
    NORMAL,
    /**
     * Administrator access
     */
    ADMIN;

    static Access fromValue(int value) {
        switch (value) {
            case 2:
                return ADMIN;
            case 1:
                return NORMAL;
        }
        return NONE;
    }
}