package net.sf.webissues.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import net.sf.webissues.api.Client.PasswordChangeCallback;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;

@Ignore
public class AbstractClientTest {
    
    final static Log LOG = LogFactory.getLog(AbstractClientTest.class);

    private String username;
    private char[] password;

    protected Operation createOperation() {
        return new Operation() {

            private boolean cancelled;
            private int value;

            public void setName(String name) {
                LOG.info("Name: " + name);
            }

            public void setCanceled(boolean cancelled) {
                this.cancelled = cancelled;
            }

            public void progressed(int value) {
                this.value = this.value += value;
                LOG.info("Progressed to: " + value);
            }

            public boolean isCanceled() {
                return cancelled;
            }

            public void done() {
                System.out.println("Done");
            }

            public void beginJob(String name, int size) {
                this.value = 0;
                LOG.info("Name: " + name + " for " + size);
            }
        };
    }
    
    protected PasswordChangeCallback createPasswordChangeCallback() {
        return new PasswordChangeCallback() {
            
            public char[] getNewPassword() {
                LOG.info("You must change you password, please enter the new one now (just press RETURN to cancel): ");
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                try {
                    String newPassword = br.readLine();
                    return newPassword == null || newPassword.length() == 0 ? null : newPassword.toCharArray();
                }
                catch(IOException ioe) {
                    ioe.printStackTrace();
                    return null;
                }
            }
        };
    }

    protected Authenticator createAuthenticator() {
        return new Authenticator() {

            public Credentials getCredentials(URL url) {
                return new Credentials() {
                    public String getUsername() {
                        return username;
                    }

                    public char[] getPassword() {
                        return password;
                    }
                };
            }
        };
    }

    protected AbstractClientTest(String username, char[] password) {
        this.username = username;
        this.password = password;
    }

    protected static void dumpClient(Client c, Operation op) throws HttpException, IOException, ProtocolException {
        LOG.info(c.getEnvironment());
        for (User user : c.getEnvironment().getUsers().values()) {
            user.reload(op);
            LOG.info("U: " + user);
        }
        for (Project project : c.getEnvironment().getProjects().values()) {
            LOG.info("P: " + project);
            for (Folder folder : project.values()) {
                LOG.info("    F: " + folder);
                for (Issue issue : folder.getIssues(op, 0)) {
                    LOG.info("        I: " + issue.toString());
                }
            }
        }
    }
}
