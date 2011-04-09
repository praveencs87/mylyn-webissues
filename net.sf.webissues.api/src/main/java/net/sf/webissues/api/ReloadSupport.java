package net.sf.webissues.api;

import java.io.IOException;

import org.apache.commons.httpclient.HttpException;

public interface ReloadSupport {
    void reload(Client client, Operation operation) throws HttpException, IOException, ProtocolException;
}
