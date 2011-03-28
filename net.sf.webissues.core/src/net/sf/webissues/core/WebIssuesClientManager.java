/*******************************************************************************
 * Copyright (c) 2006, 2008 Steffen Pingel and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/

package net.sf.webissues.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import net.sf.webissues.api.ProtocolException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.util.IdleConnectionTimeoutThread;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.commons.core.StatusHandler;
import org.eclipse.mylyn.commons.net.AbstractWebLocation;
import org.eclipse.mylyn.commons.net.WebUtil;
import org.eclipse.mylyn.tasks.core.IRepositoryListener;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.TaskRepositoryLocationFactory;

/**
 * Caches {@link ITracClient} objects.
 * 
 * @author Steffen Pingel
 */
public class WebIssuesClientManager implements IRepositoryListener {
    private static IdleConnectionTimeoutThread idleConnectionTimeoutThread = new IdleConnectionTimeoutThread();
    private static MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();

    static {
        idleConnectionTimeoutThread.addConnectionManager(connectionManager);
        idleConnectionTimeoutThread.start();
    }

    public static final String USER_AGENT = "WebIssuesConnector";

    private final Map<String, WebIssuesClient> clientByUrl = new HashMap<String, WebIssuesClient>();
    private final File cacheFile;
    private TaskRepositoryLocationFactory taskRepositoryLocationFactory;

    public WebIssuesClientManager(File cacheFile, TaskRepositoryLocationFactory taskRepositoryLocationFactory) {
        this.cacheFile = cacheFile;
        this.taskRepositoryLocationFactory = taskRepositoryLocationFactory;
        readCache();
    }

    public synchronized void validate(TaskRepository taskRepository, IProgressMonitor monitor) throws ProtocolException,
                    IOException, HttpException {
        AbstractWebLocation location = taskRepositoryLocationFactory.createWebLocation(taskRepository);
        WebIssuesClient client = new WebIssuesClient(createHttpClient(location), location);
        client.connect(monitor);
    }

    public synchronized WebIssuesClient getClient(TaskRepository taskRepository, IProgressMonitor monitor)
                    throws ProtocolException, IOException, HttpException {
        String repositoryUrl = taskRepository.getRepositoryUrl();
        WebIssuesClient client = clientByUrl.get(repositoryUrl);
        if (client == null) {
            AbstractWebLocation location = taskRepositoryLocationFactory.createWebLocation(taskRepository);
            client = new WebIssuesClient(createHttpClient(location), location);
            clientByUrl.put(repositoryUrl, client);
            client.connect(monitor);
        } else {
            if (!client.isConfigured()) {
                AbstractWebLocation location = taskRepositoryLocationFactory.createWebLocation(taskRepository);
                client.configure(createHttpClient(location), location);
            }
            if (!client.isOnline()) {
                if (!client.goOnline(monitor)) {
                    System.err.println("Failed to go back online");
                    client.getException().printStackTrace();
                }
            }
        }
        return client;
    }

    protected HttpClient createHttpClient(AbstractWebLocation location) {
        HttpClient httpClient = new HttpClient();
        httpClient.getParams().setAuthenticationPreemptive(true);
        HttpConnectionManager connectionManager = WebIssuesClientManager.getConnectionManager();
        httpClient.setHttpConnectionManager(connectionManager);
        connectionManager.getParams().setConnectionTimeout(8000);
        connectionManager.getParams().setSoTimeout(8000);
        httpClient.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
        WebUtil.configureHttpClient(httpClient, WebIssuesClientManager.USER_AGENT);
        httpClient.setHostConfiguration(WebUtil.createHostConfiguration(httpClient, location, null));
        return httpClient;
    }

    public synchronized void removeClient(TaskRepository taskRepository) {
        WebIssuesClient client = clientByUrl.get(taskRepository.getRepositoryUrl());
        if (client != null) {
            client.logout();
            clientByUrl.remove(taskRepository.getRepositoryUrl());
        }
    }

    public void repositoriesRead() {
        // ignore
    }

    public synchronized void repositoryAdded(TaskRepository repository) {
        removeClient(repository);
    }

    public synchronized void repositoryRemoved(TaskRepository repository) {
        removeClient(repository);
    }

    public synchronized void repositorySettingsChanged(TaskRepository repository) {
        removeClient(repository);
    }

    public void readCache() {
        if (cacheFile == null || !cacheFile.exists()) {
            return;
        }

        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new FileInputStream(cacheFile));
            int size = in.readInt();
            for (int i = 0; i < size; i++) {
                String url = (String) in.readObject();
                WebIssuesClient data = (WebIssuesClient) in.readObject();
                if (url != null && data != null) {
                    clientByUrl.put(url, data);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            StatusHandler.log(new Status(IStatus.WARNING, WebIssuesCorePlugin.ID_PLUGIN,
                            "The WebIssues respository configuration cache could not be read", e));
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

    }

    public void writeCache() {
        if (cacheFile == null) {
            return;
        }

        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new FileOutputStream(cacheFile));
            out.writeInt(clientByUrl.size());
            for (String url : clientByUrl.keySet()) {
                out.writeObject(url);
                out.writeObject(clientByUrl.get(url));
            }
        } catch (IOException e) {
            e.printStackTrace();
            StatusHandler.log(new Status(IStatus.WARNING, WebIssuesCorePlugin.ID_PLUGIN,
                            "The WebIssues respository configuration cache could not be written", e));
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public TaskRepositoryLocationFactory getTaskRepositoryLocationFactory() {
        return taskRepositoryLocationFactory;
    }

    public void setTaskRepositoryLocationFactory(TaskRepositoryLocationFactory taskRepositoryLocationFactory) {
        this.taskRepositoryLocationFactory = taskRepositoryLocationFactory;
    }

    public void repositoryUrlChanged(TaskRepository repository, String oldUrl) {
        // ignore
    }

    public static HttpConnectionManager getConnectionManager() {
        return connectionManager;
    }

}
