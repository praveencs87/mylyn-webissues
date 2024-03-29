package org.webissues.api;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.net.URL;

import org.junit.Ignore;
import org.webissues.api.Authenticator;
import org.webissues.api.Client;
import org.webissues.api.Operation;

@Ignore
public class SaveClientIntegrationTest extends AbstractClientTest {
    SaveClientIntegrationTest(String[] args) {
        super(args[1], args[2].toCharArray());
    }

    public static void main(final String[] args) throws Exception {
        SaveClientIntegrationTest lct = new SaveClientIntegrationTest(args);
        Client c = new Client();
        c.setUrl(new URL(args[0]));
        Operation op = lct.createOperation();
        Authenticator authenticator2 = lct.createAuthenticator();
        c.setAuthenticator(authenticator2);
        c.setPasswordChangeCallback(lct.createPasswordChangeCallback());
        c.connect(op);
        dumpClient(c, op);
        File file = new File(new File(System.getProperty("java.io.tmpdir")), "client.ser");
        FileOutputStream fos = new FileOutputStream(file);
        try {
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(c);
            oos.flush();
        } finally {
            fos.close();
        }
    }
}
