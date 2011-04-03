package net.sf.webissues.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import net.sf.webissues.api.Client.PasswordChangeCallback;

import org.apache.commons.httpclient.HttpException;
import org.junit.Ignore;

@Ignore
public class AbstractClientTest {

    private String username;
    private char[] password;

    protected Operation createOperation() {
        return new Operation() {

            private boolean cancelled;
            private int value;

            public void setName(String name) {
                System.out.println("Name: " + name);
            }

            public void setCanceled(boolean cancelled) {
                this.cancelled = cancelled;
            }

            public void progressed(int value) {
                this.value = this.value += value;
                System.out.println("Progressed to: " + value);
            }

            public boolean isCanceled() {
                return cancelled;
            }

            public void done() {
                System.out.println("Done");
            }

            public void beginJob(String name, int size) {
                this.value = 0;
                System.out.println("Name: " + name + " for " + size);
            }
        };
    }
    
    protected PasswordChangeCallback createPasswordChangeCallback() {
        return new PasswordChangeCallback() {
            
            public char[] getNewPassword() {
                System.out.println("You must change you password, please enter the new one now (just press RETURN to cancel): ");
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
        System.out.println(c.getEnvironment());
        for (User user : c.getEnvironment().getUsers().values()) {
            System.out.println("U: " + user);
        }
        for (Project project : c.getEnvironment().getProjects().values()) {
            System.out.println("P: " + project);
            for (Folder folder : project.values()) {
                System.out.println("    F: " + folder);
                for (Issue issue : folder.getIssues(op, 0)) {
                    System.out.println("        I: " + c.getIssueDetails(issue.getId(), op));
                }
            }
        }
    }
}
