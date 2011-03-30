package net.sf.webissues.api;

import java.io.IOException;

import org.apache.commons.httpclient.HttpException;

/**
 * All client operation are wrapped in this interface to allow the client to
 * catch some errors and re-attempt authentication. If this happens the
 * {@link #call()} method may be called multiple times.
 * 
 * @param <T> return value of operation
 */
public interface Call<T> {

    /**
     * Run the operation. This may get invoked multiple times if the server
     * requests re-authentication.
     * 
     * @return value
     * @throws IOException on any I/O error
     * @throws HttpException on any HTTP error
     * @throws ProtocolException on any protocol error
     */
    T call() throws IOException, HttpException, ProtocolException;
}
