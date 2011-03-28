package net.sf.webissues.api;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;

/**
 * Map of {@link Projects}s.
 */
public class Projects extends EntityMap<Project> implements Serializable {

    private static final long serialVersionUID = 1062522167070931646L;
    private final Environment environment;

    protected Projects(Environment environment) {
        super();
        this.environment = environment;
    }

    /**
     * Get the environment this project list belongs to.
     * 
     * @return environment
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * Convenience method to find a {@link Folder} with the given ID in one of
     * the projects contained in this map.
     * 
     * @param folderId folder ID
     * @return folder or <code>null</code> if no such folder exists
     */
    public Folder getFolder(int folderId) {
        for (Project project : values()) {
            Folder folder = project.get(folderId);
            if (folder != null) {
                return folder;
            }
        }
        return null;
    }

    /**
     * Reload the projects list.
     * 
     * @param operation operation 
     * @throws HttpException 
     * @throws IOException
     * @throws ProtocolException
     */
    public void reload(final Operation operation) throws HttpException, IOException, ProtocolException {
        environment.getClient().doCall(new Call<Object>() {
            public Object call() throws HttpException, IOException, ProtocolException {
                if (operation.isCanceled()) {
                    throw new ProtocolException(ProtocolException.CANCELLED);
                }
                operation.beginJob("Reloading projects", 1);
                try {
                    doReload(operation);
                } finally {
                    operation.done();
                }
                return null;
            }
        }, operation);
    }

    protected void doReload(Operation operation) throws HttpException, IOException, ProtocolException {
        Client client = environment.getClient();
        HttpMethod method = client.doCommand("LIST PROJECTS");
        try {
            Map<Integer, Project> projects = new HashMap<Integer, Project>();
            for (List<String> response : client.readResponse(method.getResponseBodyAsStream())) {
                if (response.get(0).equals("F")) {
                    int projectId = Integer.parseInt(response.get(2));
                    Project project = projects.get(projectId);
                    if (project == null) {
                        throw new Error("Expected project before folder");
                    }
                    int folderId = Integer.parseInt(response.get(1));
                    Type type = client.getEnvironment().getTypes().get(Integer.parseInt(response.get(4)));
                    project.add(new Folder(client, project, folderId, response.get(3), type, Integer.parseInt(response.get(5))));
                } else if (response.get(0).equals("P")) {
                    int projectId = Integer.parseInt(response.get(1));
                    projects.put(projectId, new Project(this, projectId, response.get(2)));
                } else {
                    Client.LOG.warn("Unexpected response \"" + response + "\"");
                }
            }
            clear();
            for (Project project : projects.values()) {
                add(project);
            }
        } finally {
            method.releaseConnection();
        }

    }
}
