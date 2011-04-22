/**
 * 
 */
package org.webissues.api;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.webissues.api.Access;
import org.webissues.api.Attachment;
import org.webissues.api.Environment;
import org.webissues.api.User;
import org.webissues.api.Util;

/**
 * {@link Attachment} test.
 * 
 * @see Attachment
 */
@Ignore
public class AttachmentIntegrationTest {

    private Environment environment;
    private User user;

    @Before
    public void createEnviroment() {
        // Create a fake user
        environment = new Environment(null);
        user = new User(environment, 123, "auser", "Mr User", Access.ADMIN);
        environment.getUsers().add(user);
    }

    /**
     * Test an attachment created
     */
    @Test
    public void testAttachmentCreatedFromResponse() {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.MILLISECOND, 0);
        List<String> row = Arrays.asList(new String[] { "A", "123", "321", "afile.txt", Util.formatTimestampInSeconds(now),
                        String.valueOf(user.getId()), "987", "An attachment" });
        Attachment attachment = Attachment.createFromResponse(null, row, environment);
        assertEquals(123, attachment.getId()); 
        System.out.println(Util.formatTimestampInSeconds(attachment.getCreatedDate()));
        assertEquals("afile.txt", attachment.getName());
        assertEquals(now, attachment.getCreatedDate());
        assertEquals(user, attachment.getCreatedUser());
        assertEquals(987, attachment.getSize());
        assertEquals("An attachment", attachment.getDescription());
    }

    /**
     * Test an attachment created with a response of incorrect length but
     * correct type
     */
    @Test(expected = IllegalArgumentException.class)
    public void testResponseWithIncorrectLength() {
        Attachment.createFromResponse(null, Arrays.asList(new String[] { "A" }), environment);
    }

    /**
     * Test an attachment created with a response of incorrect type but correct
     * length
     */
    @Test(expected = IllegalArgumentException.class)
    public void testResponseWithIncorrectType() {
        Attachment.createFromResponse(null, Arrays.asList(new String[] { "Z", "Z", "Z", "Z", "Z", "Z", "Z", "Z" }), environment);
    }

}
