package org.webissues.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

import org.junit.Ignore;
import org.webissues.api.Authenticator;
import org.webissues.api.Client;
import org.webissues.api.Operation;

@Ignore
public class LoadClientIntegrationTest extends AbstractClientTest {

    LoadClientIntegrationTest() {
        super(null, null);
    }

    public static void main(final String[] args) throws Exception {
        LoadClientIntegrationTest lct = new LoadClientIntegrationTest();
        Client c = null;
        Operation op = lct.createOperation();
        Authenticator authenticator2 = lct.createAuthenticator();
        File file = new File(new File(System.getProperty("java.io.tmpdir")), "client.ser");
        FileInputStream fin = new FileInputStream(file);
        try {
            ObjectInputStream ois = new ObjectInputStream(fin);
            c = (Client) ois.readObject();
            c.setAuthenticator(authenticator2);
        } finally {
            fin.close();
        }
        c.setAuthenticator(authenticator2);
        dumpClient(c, op);
    }
}
