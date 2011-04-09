package net.sf.webissues.api;


/**
 * Represents an alert..
 */
public class Alert implements Entity, Comparable<Alert> {

    private static final long serialVersionUID = -5170541898454267445L;

    private int id;
    private View view;
    private Folder folder;
    private boolean email;

    /**
     * Constructor.
     *
     * @param id
     * @param view
     * @param folder
     * @param email
     */
    public Alert(int id, View view, Folder folder, boolean email) {
        super();
        this.id = id;
        this.view = view;
        this.folder = folder;
        this.email = email;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof Alert && id == ((Alert) obj).id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public boolean isEmail() {
        return email;
    }

    public void setEmail(boolean email) {
        this.email = email;
    }

    @Override
    public int hashCode() {
        return getId();
    }

    public int compareTo(Alert o) {
        return Integer.valueOf(id).compareTo(Integer.valueOf(o.id));
    }

    @Override
    public String toString() {
        return "Alert [id=" + id + ", email=" + email + "]";
    }

}
