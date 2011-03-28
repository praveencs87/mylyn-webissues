package net.sf.webissues.ui;

import net.sf.webissues.api.User;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.mylyn.internal.provisional.commons.ui.CommonImages;
import org.eclipse.swt.graphics.Image;

@SuppressWarnings("restriction")
public class UserLabelProvider implements ILabelProvider {

    private User me;

    public UserLabelProvider(User me) {
        this.me = me;
    }

    public Image getImage(Object arg0) {
        if (arg0 instanceof User) {
            User owner = (User) arg0;
            if (owner.getId() == -1) {
                return CommonImages.getImage(WebIssuesImages.ALL);
            } else if (owner.equals(me)) {
                return CommonImages.getImage(CommonImages.PERSON_ME);
            } else {
                return CommonImages.getImage(CommonImages.PERSON);
            }
        }
        return null;
    }

    public String getText(Object arg0) {
        if (arg0 instanceof User) {
            User owner = (User) arg0;
            if (owner.getId() == -1) {
                return "";
            }
            return owner.getName();
        }
        return arg0.toString();
    }

    public void addListener(ILabelProviderListener arg0) {
    }

    public void dispose() {
    }

    public boolean isLabelProperty(Object arg0, String arg1) {
        return false;
    }

    public void removeListener(ILabelProviderListener arg0) {
    }

}