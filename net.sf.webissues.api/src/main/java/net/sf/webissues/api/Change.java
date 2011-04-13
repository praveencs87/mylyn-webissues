package net.sf.webissues.api;

import java.util.Calendar;
import java.util.List;

/**
 * Represents a single change made to an issue.
 */
public class Change extends AbstractChange {

    /*
     * Internal constructor.
     */
    protected Change(IssueDetails issueDetails, Type type, int id, Calendar createdDate, User createdUser, Calendar modifiedDate,
                     User modifiedUser, Attribute attribute, String oldValue, String newValue) {
        super(issueDetails, type, id, createdDate, createdUser, modifiedDate, modifiedUser, attribute, oldValue, newValue);
    }

    /*
     * Internal constructor.
     */
    protected Change(IssueDetails issueDetails, int id, Calendar createdDate, User createdUser, Calendar modifiedDate,
                     User modifiedUser, Attribute attribute, String oldValue, String newValue) {
        this(issueDetails, Type.VALUE_CHANGED, id, createdDate, createdUser, modifiedDate, modifiedUser, attribute, oldValue,
                        newValue);
    }

    /**
     * Create a change from a response of the <code>GET DETAILS</code> command.
     * The response for an attachment is in the format :-
     * 
     * <pre>
     *      H changeId issueId modifiedDate modifiedUser attributeId 'oldValue' 'newValue'
     * </pre>
     * 
     * @param response
     * @param environment
     * @return attachment
     * @throws IllegalArgumentException if response is incorrect size or not an
     *         attachment response
     */
    static Change createFromResponse(IssueDetails issueDetails, List<String> response, Users users, Environment environment) {
        if (environment.getVersion().startsWith("0.")) {
            if (response.size() != 8 || !response.get(0).equals("H")) {
                throw new IllegalArgumentException(
                                "Incorrect response. Expected 'A changeId issueId date userId attributeId 'oldValue' 'newValue'");
            }
            int attributeId = Integer.parseInt(response.get(5));
            Attribute attributeObject = environment.getTypes().getAttribute(attributeId);
            if (attributeObject == null) {
                Client.LOG.debug("Invalid change attribute ID " + attributeId + ", will assume this is the title?");
            }
            User user = users.get(Integer.parseInt(response.get(4)));
            Calendar date = Util.parseTimestampInSeconds(response.get(3));
            return new Change(issueDetails, Integer.parseInt(response.get(1)), date, user, date, user, attributeObject,
                            response.get(6), response.get(7));
        } else {
            // TODO Change needs to support the new fields
            if (response.size() != 14 || !response.get(0).equals("H")) {
                throw new IllegalArgumentException(
                                "Incorrect response. Expected 'A changeId issueId type stampId createdDate createdUser modifiedDate modifiedUser attributeId 'oldValue' 'newValue' fromFolderId toFolderId");
            }
            int attributeId = Integer.parseInt(response.get(9));
            Type type = Type.fromCode(Integer.parseInt(response.get(3)));
            Attribute attributeObject = environment.getTypes().getAttribute(attributeId);
            return new Change(issueDetails, type, Integer.parseInt(response.get(1)), Util.parseTimestampInSeconds(response.get(5)),
                            users.get(Integer.parseInt(response.get(6))), Util.parseTimestampInSeconds(response.get(7)),
                            users.get(Integer.parseInt(response.get(8))), attributeObject, response.get(10), response.get(11));
        }
    }
}
