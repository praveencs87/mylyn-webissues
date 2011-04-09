package net.sf.webissues.core;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import net.sf.webissues.api.Environment;
import net.sf.webissues.api.Type;
import net.sf.webissues.api.Util;
import net.sf.webissues.api.View;

import org.eclipse.mylyn.tasks.core.IRepositoryQuery;

public class WebIssuesFilterQueryAdapter {

    private static final long serialVersionUID = 5626098838765595799L;
    private List<WebIssuesFilterCondition> conditions = new ArrayList<WebIssuesFilterCondition>();
    private String name;
    private Type type;
    private String searchText;
    private View view;
    private boolean searchComments;

    public WebIssuesFilterQueryAdapter() {
        super();
    }

    public Collection<WebIssuesFilterCondition> getConditions() {
        return conditions;
    }

    public WebIssuesFilterQueryAdapter(IRepositoryQuery query, Environment environment) throws IOException {
        this(new URL(query.getUrl()), environment);
        name = query.getSummary();
    }

    public WebIssuesFilterQueryAdapter(URL url, Environment environment) throws IOException {
        searchComments = false;
        searchText = null;
        StringTokenizer t = new StringTokenizer(url.getQuery(), "&");
        while (t.hasMoreTokens()) {
            String name = t.nextToken();
            ;
            int idx = name.indexOf("=");
            String value = null;
            if (idx != -1) {
                value = URLDecoder.decode(name.substring(idx + 1), "UTF-8");
                name = URLDecoder.decode(name.substring(0, idx), "UTF-8");
            }
            if (name.equals("page")) {
                // Skip
            } else if (name.equals("typeId")) {
                type = environment.getTypes().get(Integer.parseInt(value));
            } else if (name.equals("name")) {
                name = value;
            } else if (name.equals("searchComments")) {
                searchComments = true;
            } else if (name.equals("searchText")) {
                searchText = value;
            } else if (name.equals("attribute")) {
                conditions.add(WebIssuesFilterCondition.fromParameterValue(value, environment));
            } else if (name.equals("viewId")) {
                view = type.getViews().getByName(value);
            }
        }
    }

    public View getView() {
        return view;
    }

    public void setView(View view) {
        this.view = view;
    }

    public WebIssuesFilterQueryAdapter(Environment environment) {
        type = environment.getTypes().size() > 0 ? environment.getTypes().values().iterator().next() : null;
    }

    public String getSearchText() {
        return searchText;
    }

    public boolean isSearchComments() {
        return searchComments;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String toQueryString() {
        StringBuffer buf = new StringBuffer();
        buf.append("searchComments");
        if (!Util.isNullOrBlank(searchText)) {
            buf.append("&searchText=" + Util.urlEncode(searchText));
        }
        buf.append("&typeId=" + type.getId());
        if (view != null) {
            buf.append("&viewId=" + view.getId());
        }
        for (WebIssuesFilterCondition condition : conditions) {
            buf.append("&attribute=" + Util.urlEncode(condition.getExpression()));
        }
        return buf.toString();
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void addCondition(WebIssuesFilterCondition condition) {
        conditions.add(condition);
    }

}
